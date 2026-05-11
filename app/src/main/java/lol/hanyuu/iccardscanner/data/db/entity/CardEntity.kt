package lol.hanyuu.iccardscanner.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import lol.hanyuu.iccardscanner.domain.model.CardType

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val idm: String,
    val type: CardType,
    val systemCode: Int,
    val nickname: String,
    val lastBalance: Int,
    val lastScannedAt: Long
)
