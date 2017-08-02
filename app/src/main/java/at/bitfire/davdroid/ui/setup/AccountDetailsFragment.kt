/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.ui.setup

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.Fragment
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.provider.CalendarContract
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import at.bitfire.davdroid.*
import at.bitfire.davdroid.log.Logger
import at.bitfire.davdroid.model.ServiceDB.*
import at.bitfire.davdroid.resource.LocalTaskList
import at.bitfire.ical4android.TaskProvider
import at.bitfire.vcard4android.GroupMethod
import kotlinx.android.synthetic.main.login_account_details.view.*
import java.util.logging.Level

class AccountDetailsFragment: Fragment() {

    companion object {

        val KEY_CONFIG = "config"

        fun newInstance(config: DavResourceFinder.Configuration): AccountDetailsFragment {
            val frag = AccountDetailsFragment()
            val args = Bundle(1)
            args.putSerializable(KEY_CONFIG, config)
            frag.arguments = args
            return frag
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.login_account_details, container, false)

        v.back.setOnClickListener({ _ ->
            fragmentManager.popBackStack()
        })

        val config = arguments.getSerializable(KEY_CONFIG) as DavResourceFinder.Configuration

        v.account_name.setText(if (config.calDAV != null && config.calDAV.email != null)
            config.calDAV.email
        else
            config.userName)

        // CardDAV-specific
        v.carddav.visibility = if (config.cardDAV != null) View.VISIBLE else View.GONE

        v.create_account.setOnClickListener({ _ ->
            val name = v.account_name.text.toString()
            if (name.isEmpty())
                v.account_name.error = getString(R.string.login_account_name_required)
            else {
                if (createAccount(name, arguments.getSerializable(KEY_CONFIG) as DavResourceFinder.Configuration)) {
                    activity.setResult(Activity.RESULT_OK)
                    activity.finish()
                } else
                    Snackbar.make(v, R.string.login_account_not_created, Snackbar.LENGTH_LONG).show()
            }
        })

        return v
    }

    private fun createAccount(accountName: String, config: DavResourceFinder.Configuration): Boolean {
        val account = Account(accountName, getString(R.string.account_type))

        // create Android account
        val userData = AccountSettings.initialUserData(config.userName)
        Logger.log.log(Level.INFO, "Creating Android account with initial config", arrayOf(account, userData))

        val accountManager = AccountManager.get(activity)
        if (!accountManager.addAccountExplicitly(account, config.password, userData))
            return false

        // add entries for account to service DB
        Logger.log.log(Level.INFO, "Writing account configuration to database", config)
        OpenHelper(activity).use { dbHelper ->
            val db = dbHelper.writableDatabase
            try {
                val settings = AccountSettings(activity, account)

                val refreshIntent = Intent(activity, DavService::class.java)
                refreshIntent.action = DavService.ACTION_REFRESH_COLLECTIONS

                if (config.cardDAV != null) {
                    // insert CardDAV service
                    val id = insertService(db, accountName, Services.SERVICE_CARDDAV, config.cardDAV)

                    // start CardDAV service detection (refresh collections)
                    refreshIntent.putExtra(DavService.EXTRA_DAV_SERVICE_ID, id)
                    activity.startService(refreshIntent)

                    // initial CardDAV account settings
                    val idx = view.contact_group_method.selectedItemPosition
                    val groupMethodName = resources.getStringArray(R.array.settings_contact_group_method_values)[idx]
                    settings.setGroupMethod(GroupMethod.valueOf(groupMethodName))

                    // contact sync is automatically enabled by isAlwaysSyncable="true" in res/xml/sync_address_books.xml
                    settings.setSyncInterval(App.addressBooksAuthority, Constants.DEFAULT_SYNC_INTERVAL)
                } else
                    ContentResolver.setIsSyncable(account, App.addressBooksAuthority, 0)

                if (config.calDAV != null) {
                    // insert CalDAV service
                    val id = insertService(db, accountName, Services.SERVICE_CALDAV, config.calDAV)

                    // start CalDAV service detection (refresh collections)
                    refreshIntent.putExtra(DavService.EXTRA_DAV_SERVICE_ID, id)
                    activity.startService(refreshIntent)

                    // calendar sync is automatically enabled by isAlwaysSyncable="true" in res/xml/sync_contacts.xml
                    settings.setSyncInterval(CalendarContract.AUTHORITY, Constants.DEFAULT_SYNC_INTERVAL)

                    // enable task sync if OpenTasks is installed
                    // further changes will be handled by PackageChangedReceiver
                    if (LocalTaskList.tasksProviderAvailable(activity)) {
                        ContentResolver.setIsSyncable(account, TaskProvider.ProviderName.OpenTasks.authority, 1)
                        settings.setSyncInterval(TaskProvider.ProviderName.OpenTasks.authority, Constants.DEFAULT_SYNC_INTERVAL)
                    }
                } else
                    ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 0)

            } catch(e: InvalidAccountException) {
                Logger.log.log(Level.SEVERE, "Couldn't access account settings", e)
            }
        }

        return true
    }

    private fun insertService(db: SQLiteDatabase, accountName: String, service: String, info: DavResourceFinder.Configuration.ServiceInfo): Long {
        // insert service
        val values = ContentValues(3)
        values.put(Services.ACCOUNT_NAME, accountName)
        values.put(Services.SERVICE, service)
        info.principal?.let { values.put(Services.PRINCIPAL, it.toString()) }
        val serviceID = db.insertWithOnConflict(Services._TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)

        // insert home sets
        for (homeSet in info.homeSets) {
            val values = ContentValues(2)
            values.put(HomeSets.SERVICE_ID, serviceID)
            values.put(HomeSets.URL, homeSet.toString())
            db.insertWithOnConflict(HomeSets._TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        // insert collections
        for (collection in info.collections.values) {
            val values = collection.toDB()
            values.put(Collections.SERVICE_ID, serviceID)
            db.insertWithOnConflict(Collections._TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        return serviceID
    }

}