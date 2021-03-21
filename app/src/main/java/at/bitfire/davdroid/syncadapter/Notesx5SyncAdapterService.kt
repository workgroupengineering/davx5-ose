package at.bitfire.davdroid.syncadapter

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import at.bitfire.davdroid.log.Logger
import at.bitfire.davdroid.model.AppDatabase
import at.bitfire.davdroid.model.Collection
import at.bitfire.davdroid.model.Service
import at.bitfire.davdroid.resource.LocalNotesx5Collection
import at.bitfire.ical4android.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.ical4android.Notesx5Collection
import at.bitfire.notesx5.NotesX5Contract
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.logging.Level

class Notesx5SyncAdapterService: SyncAdapterService() {

    override fun syncAdapter() = Notesx5SyncAdapter(this)


    class Notesx5SyncAdapter(context: Context): SyncAdapter(context) {

        override fun sync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            val client = context.contentResolver.acquireContentProviderClient(NotesX5Contract.AUTHORITY)!!

            updateLocalCollections(account, client)

            client.closeCompat()
        }


        fun updateLocalCollections(account: Account, client: ContentProviderClient) {
            val db = AppDatabase.getInstance(context)
            val service = db.serviceDao().getByAccountAndType(account.name, Service.TYPE_CALDAV)

            val remoteCollections = mutableMapOf<HttpUrl, Collection>()
            if (service != null)
                for (collection in db.collectionDao().getSyncNotesx5Collections(service.id))
                    remoteCollections[collection.url] = collection

            for (list in Notesx5Collection.find(account, client, LocalNotesx5Collection.Factory, null, null))
                list.url?.let { strUrl ->
                    val url = strUrl.toHttpUrl()
                    val info = remoteCollections[url]
                    if (info == null) {
                        Logger.log.fine("Deleting obsolete local task list $url")
                        list.delete()
                    } else {
                        // remote CollectionInfo found for this local collection, update data
                        Logger.log.log(Level.FINE, "Updating local task list $url", info)
                        list.update(info)
                        // we already have a local task list for this remote collection, don't take into consideration anymore
                        remoteCollections -= url
                    }
                }

            // create new local collections
            for ((_,info) in remoteCollections) {
                Logger.log.log(Level.INFO, "Adding local NotesX5 collections", info)
                LocalNotesx5Collection.create(account, client, info)
            }
        }

    }

}