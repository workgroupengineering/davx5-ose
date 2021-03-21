package at.bitfire.davdroid.resource

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import at.bitfire.davdroid.model.Collection
import at.bitfire.ical4android.Notesx5Collection
import at.bitfire.ical4android.Notesx5CollectionFactory
import at.bitfire.notesx5.NotesX5Contract.X5Collection
import at.bitfire.notesx5.NotesX5Contract.asSyncAdapter

class LocalNotesx5Collection(account: Account, client: ContentProviderClient): Notesx5Collection(account, client) {

    companion object {

        fun create(account: Account, client: ContentProviderClient, info: Collection) {
            val values = valuesFromCollection(info)
            client.insert(X5Collection.CONTENT_URI.asSyncAdapter(account), values)
        }

        fun valuesFromCollection(info: Collection) =
                ContentValues().apply {
                    put(X5Collection.COLUMN_COLLECTION_URL, info.url.toString())
                    put(X5Collection.COLUMN_COLLECTION_DISPLAYNAME, info.displayName ?: "letztes Segment")
                    put(X5Collection.COLUMN_COLLECTION_DESCRIPTION, info.description)
                    put(X5Collection.COLUMN_COLLECTION_OWNER, info.owner?.toString())
                    put(X5Collection.COLUMN_COLLECTION_COLOR, info.color)
                }

    }


    fun update(info: Collection) {
        val id = requireNotNull(id)
        val values = valuesFromCollection(info)
        client.update(ContentUris.withAppendedId(X5Collection.CONTENT_URI.asSyncAdapter(account), id), values, null, null)
    }


    object Factory: Notesx5CollectionFactory<LocalNotesx5Collection> {
        override fun newInstance(account: Account, client: ContentProviderClient, id: Long) = LocalNotesx5Collection(account, client)
    }

}