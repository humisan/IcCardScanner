package lol.hanyuu.iccardscanner.domain.usecase

import android.nfc.Tag
import android.nfc.tech.NfcF
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lol.hanyuu.iccardscanner.BuildConfig
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity
import lol.hanyuu.iccardscanner.data.db.entity.ScanRecordEntity
import lol.hanyuu.iccardscanner.data.nfc.FeliCaReader
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import lol.hanyuu.iccardscanner.domain.model.CardType
import javax.inject.Inject

class ReadFeliCaUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val historyRepository: HistoryRepository,
    private val detectCardType: DetectCardTypeUseCase,
    private val parseHistory: ParseTransactionHistoryUseCase
) {
    suspend operator fun invoke(tag: Tag): String = withContext(Dispatchers.IO) {
        val nfcF = NfcF.get(tag) ?: throw IllegalArgumentException("FeliCa (NfcF) tag is not available")
        val reader = FeliCaReader(nfcF)
        try {
            reader.connect()
            Log.d(
                TAG,
                "build=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) " +
                    "historyTarget=$TRANSIT_HISTORY_BLOCK_COUNT readerMaxBlocks=${reader.maxBlocksPerRead}"
            )
            val idmBytes = reader.getIdm()
            val idmHex = idmBytes.joinToString("") { "%02X".format(it) }
            val tagSystemCode = reader.getSystemCode()
            val cardLogId = idmHex.takeLast(4).padStart(idmHex.length, '*')
            Log.d(TAG, "card=$cardLogId systemCode=0x${tagSystemCode.toString(16)}")
            val availableSystemCodes = runCatching { reader.requestSystemCodes() }.getOrElse { e ->
                Log.w(TAG, "requestSystemCodes failed: ${e.message}")
                emptySet()
            }
            Log.d(TAG, "availableSystemCodes=$availableSystemCodes")
            val systemCode = resolveSystemCode(tagSystemCode, availableSystemCodes)
            var cardType = detectCardType.invoke(systemCode, idmBytes, availableSystemCodes)
            Log.d(TAG, "detected cardType=$cardType")

            if (cardType == CardType.NANACO) {
                cardType = probeNanacoOrEdy(reader)
                Log.d(TAG, "nanaco/edy probe result=$cardType")
            }

            val transitHistory = if (cardType.isTransitIc) {
                runCatching { readTransitHistoryBlocks(reader) }
                    .onFailure { e -> Log.w(TAG, "History read failed (non-fatal): ${e.message}") }
                    .getOrDefault(TransitHistoryRead(emptyList(), isComplete = false))
            } else {
                TransitHistoryRead(emptyList(), isComplete = true)
            }
            val transitBlocks = transitHistory.blocks
            if (transitBlocks.isNotEmpty()) {
                Log.d(TAG, "transitBlocks.size=${transitBlocks.size} complete=${transitHistory.isComplete}")
            }

            val balance = if (cardType.isTransitIc && transitBlocks.isNotEmpty()) {
                parseTransitBalance(transitBlocks.first())
            } else {
                readBalance(reader, cardType)
            }
            Log.d(TAG, "balance=$balance")
            if (cardType == CardType.UNKNOWN && balance < 0) {
                throw IOException("Unsupported or unstable FeliCa system code: 0x${tagSystemCode.toString(16)}")
            }
            val now = System.currentTimeMillis()

            val existingCard = cardRepository.getCard(idmHex)
            cardRepository.upsertCard(
                CardEntity(
                    idm = idmHex,
                    type = cardType,
                    systemCode = systemCode,
                    nickname = existingCard?.nickname ?: cardType.displayName,
                    lastBalance = if (balance >= 0) balance else existingCard?.lastBalance ?: -1,
                    lastScannedAt = now
                )
            )
            cardRepository.insertScanRecord(
                ScanRecordEntity(cardIdm = idmHex, scannedAt = now, balance = balance)
            )

            if (cardType.isTransitIc && transitBlocks.isNotEmpty()) {
                val records = parseHistory.invoke(idmHex, transitBlocks)
                if (transitHistory.isComplete) {
                    historyRepository.replaceTransactions(idmHex, records)
                    Log.d(TAG, "replaced ${records.size} history records")
                } else {
                    historyRepository.insertTransactionsIgnoreConflicts(records)
                    Log.w(TAG, "partial history read; merged ${records.size} records without deleting existing history")
                }
            }

            idmHex
        } finally {
            reader.close()
        }
    }

    private fun readBalance(reader: FeliCaReader, cardType: CardType): Int = try {
        when {
            cardType.isTransitIc -> {
                val block = reader.readBlocks(TRANSIT_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)
            }
            cardType == CardType.NANACO -> {
                val block = reader.readBlocks(NANACO_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[3].toInt() and 0xFF)) or
                    ((block[2].toInt() and 0xFF) shl 8) or
                    ((block[1].toInt() and 0xFF) shl 16) or
                    ((block[0].toInt() and 0xFF) shl 24)
            }
            cardType == CardType.WAON -> {
                val block = reader.readBlocks(WAON_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[1].toInt() and 0xFF) shl 8) or (block[0].toInt() and 0xFF)
            }
            cardType == CardType.EDY -> {
                val block = reader.readBlocks(EDY_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[3].toInt() and 0xFF)) or
                    ((block[2].toInt() and 0xFF) shl 8) or
                    ((block[1].toInt() and 0xFF) shl 16) or
                    ((block[0].toInt() and 0xFF) shl 24)
            }
            else -> -1
        }
    } catch (_: Exception) {
        -1
    }

    private fun readTransitHistoryBlocks(reader: FeliCaReader): TransitHistoryRead {
        val blocks = mutableListOf<ByteArray>()
        var isComplete = true
        for (index in 0 until TRANSIT_HISTORY_BLOCK_COUNT) {
            val block = runCatching { reader.readBlocks(TRANSIT_HISTORY_SERVICE_CODE, listOf(index)).first() }
                .onFailure { e -> Log.w(TAG, "History block $index read failed: ${e.message}") }
                .getOrNull()
            if (block == null) {
                isComplete = false
                break
            }
            blocks += block
        }
        Log.d(TAG, "historyRead requested=$TRANSIT_HISTORY_BLOCK_COUNT read=${blocks.size} complete=$isComplete")
        return TransitHistoryRead(blocks, isComplete)
    }

    private fun parseTransitBalance(block: ByteArray): Int =
        ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)

    private fun resolveSystemCode(tagSystemCode: Int, availableSystemCodes: Set<Int>): Int {
        if (tagSystemCode != 0) return tagSystemCode
        if (TRANSIT_SYSTEM_CODE in availableSystemCodes) return TRANSIT_SYSTEM_CODE
        return availableSystemCodes.firstOrNull() ?: tagSystemCode
    }

    private fun probeNanacoOrEdy(reader: FeliCaReader): CardType = try {
        reader.readBlocks(NANACO_BALANCE_SERVICE_CODE, listOf(0))
        CardType.NANACO
    } catch (_: Exception) {
        try {
            reader.readBlocks(EDY_BALANCE_SERVICE_CODE, listOf(0))
            CardType.EDY
        } catch (_: Exception) {
            CardType.UNKNOWN
        }
    }

    private companion object {
        const val TAG = "ReadFeliCa"
        const val TRANSIT_SYSTEM_CODE = 0x0003
        const val TRANSIT_BALANCE_SERVICE_CODE = 0x008B
        const val TRANSIT_HISTORY_SERVICE_CODE = 0x090F
        const val TRANSIT_HISTORY_BLOCK_COUNT = 20
        const val NANACO_BALANCE_SERVICE_CODE = 0x564F
        const val WAON_BALANCE_SERVICE_CODE = 0x6817
        const val EDY_BALANCE_SERVICE_CODE = 0x170F
    }

    private data class TransitHistoryRead(
        val blocks: List<ByteArray>,
        val isComplete: Boolean
    )
}
