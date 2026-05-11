package lol.hanyuu.iccardscanner.domain.usecase

import java.util.Calendar
import java.util.GregorianCalendar
import javax.inject.Inject
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity
import lol.hanyuu.iccardscanner.domain.model.ProcessType

class ParseTransactionHistoryUseCase @Inject constructor() {

    fun invoke(cardIdm: String, blocks: List<ByteArray>): List<TransactionRecordEntity> =
        blocks.mapNotNull { block -> parseBlock(cardIdm, block) }

    private fun parseBlock(cardIdm: String, block: ByteArray): TransactionRecordEntity? {
        if (block.size < 16) return null
        val terminalType = block[0].toInt() and 0xFF
        val processCode = block[1].toInt() and 0xFF
        if (terminalType == 0 && processCode == 0) return null

        val dateWord = ((block[2].toInt() and 0xFF) shl 8) or (block[3].toInt() and 0xFF)
        val year = (dateWord ushr 9) + 2000
        val month = (dateWord ushr 5) and 0x0F
        val day = dateWord and 0x1F
        if (month !in 1..12 || day !in 1..31) return null

        val transactionDate = GregorianCalendar().apply {
            isLenient = false
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.runCatchingTimeInMillis() ?: return null

        val balance = ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)
        val amount = ((block[13].toInt() and 0xFF) shl 8) or (block[12].toInt() and 0xFF)

        val processType = when (processCode) {
            0x01 -> ProcessType.ENTRY
            0x02 -> ProcessType.EXIT
            0x0F, 0x1F -> ProcessType.ENTRY
            0x23 -> ProcessType.PURCHASE
            0x46 -> ProcessType.CHARGE
            else -> ProcessType.OTHER
        }

        val entryLine = block[4].toInt() and 0xFF
        val entryStation = block[5].toInt() and 0xFF
        val exitLine = block[6].toInt() and 0xFF
        val exitStation = block[7].toInt() and 0xFF
        val isTransit = processType == ProcessType.ENTRY || processType == ProcessType.EXIT

        return TransactionRecordEntity(
            cardIdm = cardIdm,
            transactionDate = transactionDate,
            processType = processType,
            amount = amount,
            balance = balance,
            entryStationCode = if (isTransit) encodeStation(entryLine, entryStation) else null,
            exitStationCode = if (isTransit) encodeStation(exitLine, exitStation) else null,
            details = null
        )
    }

    private fun GregorianCalendar.runCatchingTimeInMillis(): Long? =
        runCatching { timeInMillis }.getOrNull()

    private fun encodeStation(line: Int, station: Int): Int = (line shl 8) or station
}
