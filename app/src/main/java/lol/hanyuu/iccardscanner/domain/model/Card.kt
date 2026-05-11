package lol.hanyuu.iccardscanner.domain.model

data class Card(
    val idm: String,
    val type: CardType,
    val nickname: String,
    val lastBalance: Int,
    val lastScannedAt: Long
)
