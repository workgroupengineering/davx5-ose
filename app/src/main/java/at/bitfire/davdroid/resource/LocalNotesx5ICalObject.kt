package at.bitfire.davdroid.resource

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import at.bitfire.davdroid.DavUtils
import at.bitfire.davdroid.model.Collection
import at.bitfire.ical4android.Notesx5Collection
import at.bitfire.ical4android.Notesx5CollectionFactory
import at.bitfire.ical4android.Notesx5ICalObject
import at.bitfire.ical4android.Notesx5ICalObjectFactory
import at.bitfire.notesx5.NotesX5Contract
import at.bitfire.notesx5.NotesX5Contract.X5Collection
import at.bitfire.notesx5.NotesX5Contract.asSyncAdapter

class LocalNotesx5ICalObject(account: Account, client: ContentProviderClient, id: Long, component: NotesX5Contract.X5ICalObject.Component): Notesx5ICalObject(account, client, id, component) {

    companion object {

        /*
        fun create(account: Account, client: ContentProviderClient, info: Collection) {
            val values = valuesFromCollection(info, account)
            create(account, client, values)
        }



        fun valuesFromCollection(info: Collection, account: Account) =
                ContentValues().apply {
                    put(X5Collection.URL, info.url.toString())
                    put(X5Collection.DISPLAYNAME, info.displayName ?: DavUtils.lastSegmentOfUrl(info.url))
                    put(X5Collection.DESCRIPTION, info.description)
                    put(X5Collection.OWNER, info.owner?.toString())
                    put(X5Collection.COLOR, info.color)
                    put(X5Collection.SUPPORTSVEVENT, info.supportsVEVENT)
                    put(X5Collection.SUPPORTSVJOURNAL, info.supportsVJOURNAL)
                    put(X5Collection.SUPPORTSVTODO, info.supportsVTODO)
                    put(X5Collection.ACCOUNT_NAME, account.name)
                    put(X5Collection.ACCOUNT_TYPE, account.type)
                    put(X5Collection.READONLY, info.forceReadOnly)
                }

         */
    }

/*
    fun update(info: Collection) {
        val id = requireNotNull(id)
        val values = valuesFromCollection(info, account)
        client.update(ContentUris.withAppendedId(X5Collection.CONTENT_URI.asSyncAdapter(account), id), values, null, null)
    }


 */

    object Factory: Notesx5ICalObjectFactory<LocalNotesx5ICalObject> {
       // override fun newInstance(account: Account, client: ContentProviderClient, id: Long) = LocalNotesx5ICalObject(account, client, id)

        override fun fromProvider(iCalObject: Notesx5Collection<Notesx5ICalObject>, values: ContentValues): LocalNotesx5ICalObject {
            TODO("Not yet implemented")
        }
    }

}