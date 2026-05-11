package lol.hanyuu.iccardscanner.domain.usecase

import android.nfc.Tag
import android.nfc.tech.NfcF
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val idmBytes = reader.getIdm()
            val idmHex = idmBytes.joinToString("") { "%02X".format(it) }
            val systemCode = reader.getSystemCode()
            var cardType = detectCardType.invoke(systemCode, idmBytes)

            if (cardType == CardType.NANACO) {
                cardType = probeNanacoOrEdy(reader)
            }

            val transitBlocks = if (cardType.isTransitIc) {
                reader.readBlocks(TRANSIT_HISTORY_SERVICE_CODE, (0 until TRANSIT_HISTORY_BLOCK_COUNT).toList())
            } else {
                emptyList()
            }
            val balance = if (cardType.isTransitIc) {
                transitBlocks.firstOrNull()?.let(::parseStoredValueBalance)
                    ?: throw IOException("Transit IC history block is empty")
            } else {
                readBalance(reader, cardType)
            }
            val now = System.currentTimeMillis()

            val existingCard = cardRepository.getCard(idmHex)
            cardRepository.upsertCard(
                CardEntity(
                    idm = idmHex,
                    type = cardType,
                    systemCode = systemCode,
                    nickname = existingCard?.nickname ?: cardType.displayName,
                    lastBalance = balance,
                    lastScannedAt = now
                )
            )
            cardRepository.insertScanRecord(
                ScanRecordEntity(cardIdm = idmHex, scannedAt = now, balance = balance)
            )

            if (cardType.isTransitIc) {
                val records = parseHistory.invoke(idmHex, transitBlocks)
                historyRepository.insertTransactionsIgnoreConflicts(records)
            }

            idmHex
        } finally {
            reader.close()
        }
    }

    private fun readBalance(reader: FeliCaReader, cardType: CardType): Int = try {
        when (cardType) {
            CardType.NANACO -> {
                val block = reader.readBlocks(NANACO_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[3].toInt() and 0xFF)) or
                    ((block[2].toInt() and 0xFF) shl 8) or
                    ((block[1].toInt() and 0xFF) shl 16) or
                    ((block[0].toInt() and 0xFF) shl 24)
            }
            CardType.WAON -> {
                val block = reader.readBlocks(WAON_BALANCE_SERVICE_CODE, listOf(0)).first()
                ((block[1].toInt() and 0xFF) shl 8) or (block[0].toInt() and 0xFF)
            }
            CardType.EDY -> {
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

    private fun parseStoredValueBalance(block: ByteArray): Int {
        if (block.size < 12) throw IOException("Transit IC block is too short: ${block.size}")
        return ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)
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
        const val TRANSIT_HISTORY_SERVICE_CODE = 0x090F
        const val TRANSIT_HISTORY_BLOCK_COUNT = 20
        const val NANACO_BALANCE_SERVICE_CODE = 0x564F
        const val WAON_BALANCE_SERVICE_CODE = 0x6817
        const val EDY_BALANCE_SERVICE_CODE = 0x170F
    }
}
