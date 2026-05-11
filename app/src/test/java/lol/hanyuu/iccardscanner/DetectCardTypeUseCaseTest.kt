package lol.hanyuu.iccardscanner

import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.domain.usecase.DetectCardTypeUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectCardTypeUseCaseTest {
    private val useCase = DetectCardTypeUseCase()

    @Test
    fun `system code 0x0003 with PASMO issuer hint returns PASMO`() {
        assertEquals(CardType.PASMO, useCase.invoke(0x0003, byteArrayOf(0x00, 0x08)))
    }

    @Test
    fun `system code 0x0003 without issuer hint falls back to SUICA`() {
        assertEquals(CardType.SUICA, useCase.invoke(0x0003, byteArrayOf(0x01)))
    }

    @Test
    fun `system code 0xFE00 returns WAON`() {
        assertEquals(CardType.WAON, useCase.invoke(0xFE00, byteArrayOf()))
    }

    @Test
    fun `unknown system code returns UNKNOWN`() {
        assertEquals(CardType.UNKNOWN, useCase.invoke(0x1234, byteArrayOf()))
    }
}
