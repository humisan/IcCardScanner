package lol.hanyuu.iccardscanner.domain.usecase

import java.util.Calendar
import java.util.GregorianCalendar
import javax.inject.Inject
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity
import lol.hanyuu.iccardscanner.domain.model.ProcessType

class ParseTransactionHistoryUseCase @Inject constructor() {

    fun invoke(cardIdm: String, blocks: List<ByteArray>): List<TransactionRecordEntity> {
        val parsedBlocks = blocks.mapNotNull { block -> parseBlock(block) }
        return parsedBlocks.mapIndexed { index, parsed ->
            val older = parsedBlocks.getOrNull(index + 1)
            val amount = older?.let { kotlin.math.abs(parsed.balance - it.balance) } ?: 0
            parsed.toEntity(cardIdm, amount)
        }
    }

    private fun parseBlock(block: ByteArray): ParsedTransitBlock? {
        if (block.size < 16) return null
        val terminalType = block[0].toInt() and 0xFF
        val processCode = block[1].toInt() and 0xFF
        if (terminalType == 0 && processCode == 0) return null

        val dateWord = ((block[4].toInt() and 0xFF) shl 8) or (block[5].toInt() and 0xFF)
        val year = (dateWord ushr 9) + 2000
        val month = (dateWord ushr 5) and 0x0F
        val day = dateWord and 0x1F
        if (month !in 1..12 || day !in 1..31) return null

        val transactionDate = GregorianCalendar().apply {
            isLenient = false
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.runCatchingTimeInMillis() ?: return null

        val processType = when (processCode) {
            0x01 -> ProcessType.EXIT
            0x02 -> ProcessType.CHARGE
            0x0F, 0x1F -> ProcessType.ENTRY
            0x03, 0x23 -> ProcessType.PURCHASE
            0x46 -> ProcessType.CHARGE
            else -> ProcessType.OTHER
        }

        val entryLine = block[6].toInt() and 0xFF
        val entryStation = block[7].toInt() and 0xFF
        val exitLine = block[8].toInt() and 0xFF
        val exitStation = block[9].toInt() and 0xFF
        val balance = ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)
        val region = block[15].toInt() and 0xFF
        val sequence = ((block[13].toInt() and 0xFF) shl 8) or (block[12].toInt() and 0xFF)

        return ParsedTransitBlock(
            transactionDate = transactionDate,
            processType = processType,
            balance = balance,
            entryStationCode = encodeStation(entryLine, entryStation),
            exitStationCode = encodeStation(exitLine, exitStation),
            details = buildDetails(terminalType, processCode, region, sequence)
        )
    }

    private fun ParsedTransitBlock.toEntity(cardIdm: String, amount: Int): TransactionRecordEntity =
        TransactionRecordEntity(
            cardIdm = cardIdm,
            transactionDate = transactionDate,
            processType = processType,
            amount = amount,
            balance = balance,
            entryStationCode = entryStationCode,
            exitStationCode = exitStationCode,
            details = details
        )

    private fun GregorianCalendar.runCatchingTimeInMillis(): Long? =
        runCatching { timeInMillis }.getOrNull()

    private fun encodeStation(line: Int, station: Int): Int? {
        if (line == 0 && station == 0) return null
        return (line shl 8) or station
    }

    private fun buildDetails(
        terminalType: Int,
        processCode: Int,
        region: Int,
        sequence: Int
    ): String =
        "端末=${terminalType.toTerminalName()} / 処理=0x${processCode.toHex()} / 地域=0x${region.toHex()} / 連番=$sequence"

    private fun Int.toHex(): String = toString(16).padStart(2, '0')

    private fun Int.toTerminalName(): String = when (this) {
        0x03 -> "精算機"
        0x05 -> "バス"
        0x07, 0x08, 0x09, 0x12, 0x13, 0x14, 0x15 -> "券売機"
        0x16 -> "改札機"
        0x17 -> "簡易改札"
        0x18, 0x19 -> "窓口"
        0x1C, 0x1D -> "乗継精算機"
        0x46, 0x48 -> "ATM"
        0xC7 -> "物販端末"
        0xC8 -> "自販機"
        else -> "0x${toHex()}"
    }

    private data class ParsedTransitBlock(
        val transactionDate: Long,
        val processType: ProcessType,
        val balance: Int,
        val entryStationCode: Int?,
        val exitStationCode: Int?,
        val details: String
    )
}
