package lol.hanyuu.iccardscanner.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_records",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["idm"],
            childColumns = ["cardIdm"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardIdm")]
)
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardIdm: String,
    val scannedAt: Long,
    val balance: Int
)
