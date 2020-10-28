package at.bitfire.davdroid.model

import androidx.room.*
import at.bitfire.davdroid.log.Logger
import java.util.*


@Entity(tableName = "collectionsyncinfo",

        foreignKeys = [
            ForeignKey(entity = Collection::class, parentColumns = arrayOf("id"), childColumns = arrayOf("collectionId"), onDelete = ForeignKey.CASCADE)
        ],
        indices = [
            // index by collectionId and syncAuthority, one collectionId can contain 2 or more syncAuthorities
            Index("collectionId", "syncAuthority", unique = true)
        ]
)


data class CollectionSyncInfo(
        @PrimaryKey(autoGenerate = true)
        var id: Long,

        var collectionId: Long,
        var syncAuthority: String,

        var lastSyncTimestamp: Long?,
)

