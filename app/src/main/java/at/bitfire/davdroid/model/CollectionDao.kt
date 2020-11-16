package at.bitfire.davdroid.model

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CollectionDao: SyncableDao<Collection> {

    @Query("SELECT * FROM collection WHERE id=:id")
    fun get(id: Long): Collection?

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId")
    fun getByService(serviceId: Long): List<Collection>

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId AND type=:type")
    fun getByServiceAndType(serviceId: Long, type: String): List<Collection>

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId AND type=:type ORDER BY displayName, url")
    fun pageByServiceAndType(serviceId: Long, type: String): DataSource.Factory<Int, Collection>

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId AND sync ORDER BY displayName, url")
    fun getByServiceAndSync(serviceId: Long): List<Collection>

    @Query("SELECT collection.* FROM collection, homeset WHERE collection.serviceId=:serviceId AND type=:type AND homeSetId=homeset.id AND homeset.personal ORDER BY collection.displayName, collection.url")
    fun pagePersonalByServiceAndType(serviceId: Long, type: String): DataSource.Factory<Int, Collection>

    @Query("SELECT COUNT(*) FROM collection WHERE serviceId=:serviceId AND sync")
    fun observeHasSyncByService(serviceId: Long): LiveData<Boolean>

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId AND type='${Collection.TYPE_CALENDAR}' AND supportsVEVENT AND sync ORDER BY displayName, url")
    fun getSyncCalendars(serviceId: Long): List<Collection>

    @Query("SELECT * FROM collection WHERE serviceId=:serviceId AND type='${Collection.TYPE_CALENDAR}' AND supportsVTODO AND sync ORDER BY displayName, url")
    fun getSyncTaskLists(serviceId: Long): List<Collection>

    @Query("SELECT * FROM collection WHERE url=:url")
    fun getByUrl(url: String): Collection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(collection: Collection)

    @Insert
    fun insert(collection: Collection)

}
