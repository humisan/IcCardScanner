package lol.hanyuu.iccardscanner.domain.usecase

import lol.hanyuu.iccardscanner.domain.model.CardType
import javax.inject.Inject

class DetectCardTypeUseCase @Inject constructor() {

    fun invoke(
        systemCode: Int,
        idmBytes: ByteArray,
        availableSystemCodes: Set<Int> = emptySet()
    ): CardType = when (systemCode) {
        0x0003 -> detectTransitIc(idmBytes, availableSystemCodes)
        0xFE00 -> CardType.WAON
        0x88B4 -> CardType.NANACO
        else -> CardType.UNKNOWN
    }

    // IDm byte[1] is a commonly used issuer hint for Japanese transit IC cards.
    // The system code remains 0x0003 for the interoperable transit card family.
    private fun detectTransitIc(idmBytes: ByteArray, availableSystemCodes: Set<Int>): CardType {
        if (SUICA_SPECIFIC_SYSTEM_CODE in availableSystemCodes) return CardType.SUICA
        if (idmBytes.size < 2) return CardType.ICOCA
        return when (idmBytes[1].toInt() and 0xFF) {
            0x07 -> CardType.ICOCA
            0x08 -> CardType.PASMO
            0x09 -> CardType.TOICA
            0x17 -> CardType.SUICA
            0x1A -> CardType.KITACA
            0x1B -> CardType.MANACA
            0x23 -> CardType.SUGOCA
            0x24 -> CardType.NIMOCA
            0x25 -> CardType.HAYAKAKEN
            else -> CardType.ICOCA
        }
    }

    private companion object {
        const val SUICA_SPECIFIC_SYSTEM_CODE = 0x86A7
    }
}
