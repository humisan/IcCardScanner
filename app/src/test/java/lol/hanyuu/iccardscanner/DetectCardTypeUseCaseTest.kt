package lol.hanyuu.iccardscanner

import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.domain.usecase.DetectCardTypeUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectCardTypeUseCaseTest {
    private val useCase = DetectCardTypeUseCase()

    @Test
    fun `system code 0x0003 returns SUICA`() {
        assertEquals(CardType.SUICA, useCase.invoke(0x0003))
    }

    @Test
    fun `system code 0xFE00 returns WAON`() {
        assertEquals(CardType.WAON, useCase.invoke(0xFE00))
    }

    @Test
    fun `unknown system code returns UNKNOWN`() {
        assertEquals(CardType.UNKNOWN, useCase.invoke(0x1234))
    }
}
