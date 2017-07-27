package at.bitfire.davdroid.syncadapter

import android.app.PendingIntent
import android.content.*
import android.os.DeadObjectException
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import at.bitfire.davdroid.*
import at.bitfire.davdroid.ui.DebugInfoActivity
import at.bitfire.davdroid.ui.SubscriptionActivity
import com.android.vending.billing.IInAppBillingService
import java.util.logging.Level

class ICloudSyncPlugin: ISyncPlugin {

    var billingService: IInAppBillingService? = null
    val billingServiceLock = Object()

    val billingServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(billingServiceLock) {
                billingService = IInAppBillingService.Stub.asInterface(service);
                billingServiceLock.notify()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            billingService = null
        }
    };


    override fun beforeSync(context: Context, syncResult: SyncResult): Boolean {
        // connect to Google Play billing service
        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.`package` = "com.android.vending"
        if (!context.bindService(serviceIntent, billingServiceConnection, Context.BIND_AUTO_CREATE)) {
            App.log.severe("Couldn't connect to Google Play billing service")
            return false
        }

        // wait for billing service
        synchronized(billingServiceLock) {
            if (billingService == null)
                try {
                    billingServiceLock.wait()
                } catch(e: InterruptedException) {
                    App.log.log(Level.SEVERE, "Couldn't wait for Google Play billing service", e)
                    return false
                }
        }

        var exception: Exception? = null

        try {
            billingService?.let {
                val subscription = SubscriptionManager(context, it)
                if (subscription.isValid()) {
                    App.log.info("Valid license found: ${subscription.info.status}")
                    return true
                }
            }
        } catch(e: BillingException) {
            App.log.log(Level.WARNING, "Couldn't determine subscription state", e)
            if (e.cause is DeadObjectException) {
                // ignore DeadObjectExceptions and restart sync as soon as possible
                syncResult.stats.numIoExceptions++
                return false
            } else
                exception = e
        }

        // no valid license found, show notification
        val notify = NotificationCompat.Builder(context)
                .setLargeIcon(App.getLauncherBitmap(context))
                .setSmallIcon(R.drawable.ic_account_circle_white)
                .setContentTitle(context.getString(R.string.subscription_notification_title))
                .setContentText(context.getString(R.string.subscription_notification_text))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, SubscriptionActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

        exception?.let {
            val intent = Intent(context, DebugInfoActivity::class.java)
            intent.putExtra(DebugInfoActivity.KEY_THROWABLE, it)
            notify.addAction(R.drawable.ic_error_light, context.getString(R.string.subscription_notification_show_details),
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
        }

        val nm = NotificationManagerCompat.from(context)
        nm.notify(Constants.NOTIFICATION_SUBSCRIPTION, notify.build())

        return false
    }

    override fun afterSync(context: Context, syncResult: SyncResult) {
        billingService?.let {
            try {
                context.unbindService(billingServiceConnection)
            } catch(e: IllegalArgumentException) {
                App.log.log(Level.SEVERE, "Couldn't unbind Google Play service", e)
            }
        }
    }

}