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

}