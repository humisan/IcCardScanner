package lol.hanyuu.iccardscanner.domain.usecase

import lol.hanyuu.iccardscanner.domain.model.CardType
import javax.inject.Inject

class DetectCardTypeUseCase @Inject constructor() {
    fun invoke(systemCode: Int): CardType = when (systemCode) {
        0x0003 -> CardType.SUICA
        0xFE00 -> CardType.WAON
        0x88B4 -> CardType.NANACO
        else -> CardType.UNKNOWN
    }
}
