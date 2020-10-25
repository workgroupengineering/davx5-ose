/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package at.bitfire.davdroid.syncadapter

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.provider.CalendarContract
import at.bitfire.davdroid.log.Logger
import at.bitfire.davdroid.model.AppDatabase
import at.bitfire.davdroid.model.Collection
import at.bitfire.davdroid.model.CollectionSyncInfo
import at.bitfire.davdroid.model.Service
import at.bitfire.davdroid.resource.LocalCalendar
import at.bitfire.davdroid.settings.AccountSettings
import at.bitfire.ical4android.AndroidCalendar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.LocalDateTime
import java.util.logging.Level

class CalendarsSyncAdapterService: SyncAdapterService() {

    override fun syncAdapter() = CalendarsSyncAdapter(this)


	class CalendarsSyncAdapter(
            context: Context
    ): SyncAdapter(context) {

        override fun sync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            try {
                val accountSettings = AccountSettings(context, account)

                /* don't run sync if
                   - sync conditions (e.g. "sync only in WiFi") are not met AND
                   - this is is an automatic sync (i.e. manual syncs are run regardless of sync conditions)
                 */
                if (!extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL) && !checkSyncConditions(accountSettings))
                    return

                if (accountSettings.getEventColors())
                    AndroidCalendar.insertColors(provider, account)
                else
                    AndroidCalendar.removeColors(provider, account)

                updateLocalCalendars(provider, account, accountSettings)

                val priorityCalendars = priorityCollections(extras)
                val calendars = AndroidCalendar
                        .find(account, provider, LocalCalendar.Factory, "${CalendarContract.Calendars.SYNC_EVENTS}!=0", null)
                        .sortedByDescending { priorityCalendars.contains(it.id) }
                for (calendar in calendars) {
                    Logger.log.info("Synchronizing calendar #${calendar.id}, URL: ${calendar.name}")
                    CalendarSyncManager(context, account, accountSettings, extras, authority, syncResult, calendar).use {
                        it.performSync()
                        Logger.log.info("Sync-Result for calendar #${calendar.id}, URL: ${calendar.name}: ${it.syncResult.stats.numEntries} processed, ${it.syncResult.stats.numDeletes} deleted, ${it.syncResult.stats.numInserts} inserted, ${it.syncResult.stats.numUpdates} updated")

                        //Store Sync-Info for later display
                        val db = AppDatabase.getInstance(context)
                        val collection = calendar.name?.let { url -> db.collectionDao().getByUrl(url) }
                        if (collection != null) {
                            db.collectionSyncInfoDao().insertOrReplace(
                                    CollectionSyncInfo(0, collection!!.id, authority, System.currentTimeMillis(), it.syncResult.stats.numDeletes + it.syncResult.stats.numInserts + it.syncResult.stats.numUpdates)
                            )
                        }


                    }
                }
            } catch(e: Exception) {
                Logger.log.log(Level.SEVERE, "Couldn't sync calendars", e)
            }
            Logger.log.info("Calendar sync complete")

        }

        private fun updateLocalCalendars(provider: ContentProviderClient, account: Account, settings: AccountSettings) {
            val db = AppDatabase.getInstance(context)
            val service = db.serviceDao().getByAccountAndType(account.name, Service.TYPE_CALDAV)


            val remoteCalendars = mutableMapOf<HttpUrl, Collection>()
            if (service != null)
                for (collection in db.collectionDao().getSyncCalendars(service.id)) {
                    remoteCalendars[collection.url] = collection
                }

            // delete/update local calendars
            val updateColors = settings.getManageCalendarColors()
            for (calendar in AndroidCalendar.find(account, provider, LocalCalendar.Factory, null, null))
                calendar.name?.let {
                    val url = it.toHttpUrl()
                    val info = remoteCalendars[url]
                    if (info == null) {
                        Logger.log.log(Level.INFO, "Deleting obsolete local calendar", url)
                        calendar.delete()
                    } else {
                        // remote CollectionInfo found for this local collection, update data
                        Logger.log.log(Level.FINE, "Updating local calendar $url", info)
                        calendar.update(info, updateColors)
                        // we already have a local calendar for this remote collection, don't take into consideration anymore
                        remoteCalendars -= url
                    }
                }

            // create new local calendars
            for ((_, info) in remoteCalendars) {
                Logger.log.log(Level.INFO, "Adding local calendar", info)
                LocalCalendar.create(account, provider, info)
            }

        }

    }

}
