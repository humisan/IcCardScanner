package lol.hanyuu.iccardscanner.domain.usecase

import android.nfc.Tag
import android.nfc.tech.NfcF
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
    /**
     * Returns the IDm hex string of the scanned card.
     * Throws IOException on NFC errors, IllegalArgumentException if not FeliCa.
     */
    suspend operator fun invoke(tag: Tag): String {
        val nfcF = NfcF.get(tag) ?: throw IllegalArgumentException("FeliCa (NfcF) タグではありません")
        val reader = FeliCaReader(nfcF)
        try {
            reader.connect()
            val idmBytes = reader.getIdm()
            val idmHex = idmBytes.joinToString("") { "%02X".format(it) }
            val systemCode = reader.getSystemCode()
            var cardType = detectCardType.invoke(systemCode)

            // 0x88B4 is nanaco or Edy — probe service codes to distinguish
            if (cardType == CardType.NANACO) {
                cardType = probeNanacoOrEdy(reader)
            }

            val balance = readBalance(reader, cardType)
            val now = System.currentTimeMillis()

            val existingCard = cardRepository.getCard(idmHex)
            cardRepository.upsertCard(
                CardEntity(
                    idm = idmHex,
                    type = if (existingCard != null) existingCard.type else cardType,
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
                val blocks = reader.readBlocks(0x090F, (0 until 20).toList())
                val records = parseHistory.invoke(idmHex, blocks)
                historyRepository.insertTransactionsIgnoreConflicts(records)
            }

            return idmHex
        } finally {
            reader.close()
        }
    }

    private fun readBalance(reader: FeliCaReader, cardType: CardType): Int = try {
        when {
            cardType.isTransitIc -> {
                val block = reader.readBlocks(0x008B, listOf(0)).first()
                ((block[11].toInt() and 0xFF) shl 8) or (block[10].toInt() and 0xFF)
            }
            cardType == CardType.NANACO -> {
                // NOTE: service code 0x564F and byte layout need real-hardware verification
                val block = reader.readBlocks(0x564F, listOf(0)).first()
                ((block[3].toInt() and 0xFF)) or
                ((block[2].toInt() and 0xFF) shl 8) or
                ((block[1].toInt() and 0xFF) shl 16) or
                ((block[0].toInt() and 0xFF) shl 24)
            }
            cardType == CardType.WAON -> {
                // NOTE: service code 0x6817 and byte layout need real-hardware verification
                val block = reader.readBlocks(0x6817, listOf(0)).first()
                ((block[1].toInt() and 0xFF) shl 8) or (block[0].toInt() and 0xFF)
            }
            cardType == CardType.EDY -> {
                // NOTE: service code 0x170F and byte layout need real-hardware verification
                val block = reader.readBlocks(0x170F, listOf(0)).first()
                ((block[3].toInt() and 0xFF)) or
                ((block[2].toInt() and 0xFF) shl 8) or
                ((block[1].toInt() and 0xFF) shl 16) or
                ((block[0].toInt() and 0xFF) shl 24)
            }
            else -> -1
        }
    } catch (_: Exception) { -1 }

    private fun probeNanacoOrEdy(reader: FeliCaReader): CardType = try {
        reader.readBlocks(0x564F, listOf(0))
        CardType.NANACO
    } catch (_: Exception) {
        try {
            reader.readBlocks(0x170F, listOf(0))
            CardType.EDY
        } catch (_: Exception) {
            CardType.UNKNOWN
        }
    }
}
