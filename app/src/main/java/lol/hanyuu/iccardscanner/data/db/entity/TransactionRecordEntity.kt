package lol.hanyuu.iccardscanner.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import lol.hanyuu.iccardscanner.domain.model.ProcessType

@Entity(
    tableName = "transaction_records",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["idm"],
            childColumns = ["cardIdm"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cardIdm"),
        // sequence replaces amount in the unique key: amount is derived from consecutive balances
        // and can differ between partial reads and full reads for the same transaction.
        Index(
            name = "index_transaction_records_history_key",
            value = ["cardIdm", "transactionDate", "processType", "balance", "entryStationCode", "exitStationCode", "sequence"],
            unique = true
        )
    ]
)
data class TransactionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardIdm: String,
    val transactionDate: Long,
    val processType: ProcessType,
    val amount: Int,
    val balance: Int,
    val entryStationCode: Int?,
    val exitStationCode: Int?,
    val sequence: Int,
    val details: String?
)
