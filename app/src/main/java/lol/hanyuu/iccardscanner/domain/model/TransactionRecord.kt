package lol.hanyuu.iccardscanner.domain.model

data class TransactionRecord(
    val id: Long,
    val cardIdm: String,
    val transactionDate: Long,
    val processType: ProcessType,
    val amount: Int,
    val balance: Int,
    val entryStationCode: Int?,
    val exitStationCode: Int?,
    val details: String?
)
