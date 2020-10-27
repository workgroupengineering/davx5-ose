package at.bitfire.davdroid.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CollectionSyncInfoDao {

    @Query("SELECT * FROM collectionsyncinfo WHERE collectionId=:collectionId")
    fun getLastSyncTimestampByCollection(collectionId: Long): List<CollectionSyncInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(collectionSyncInfo: CollectionSyncInfo)


/*

    @Query("DELETE FROM collectionsyncinfo")
    fun deleteAll()

    @Query("SELECT id FROM service WHERE accountName=:accountName AND type=:type")
    fun getIdByAccountAndType(accountName: String, type: String): Long?

    @Query("SELECT * FROM service WHERE id=:id")
    fun get(id: Long): Service?

    @Query("SELECT * FROM service WHERE type=:type")
    fun getByType(type: String): List<Service>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(service: Service): Long

    @Query("DELETE FROM service WHERE accountName NOT IN (:accountNames)")
    fun deleteExceptAccounts(accountNames: Array<String>)

    @Query("UPDATE service SET accountName=:newName WHERE accountName=:oldName")
    fun renameAccount(oldName: String, newName: String)
    */
}