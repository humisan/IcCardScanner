package lol.hanyuu.iccardscanner

import lol.hanyuu.iccardscanner.domain.model.ProcessType
import lol.hanyuu.iccardscanner.domain.usecase.ParseTransactionHistoryUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseTransactionHistoryUseCaseTest {
    private val useCase = ParseTransactionHistoryUseCase()
    private val testIdm = "AABBCCDD11223344"

    @Test
    fun `parses date balance and amount from block`() {
        val block = ByteArray(16).apply {
            this[1] = 0x01
            this[2] = 0x30
            this[3] = 0x6F
            this[10] = 0x88.toByte()
            this[11] = 0x13
            this[12] = 0xC8.toByte()
            this[13] = 0x00
        }

        val records = useCase.invoke(testIdm, listOf(block))

        assertEquals(1, records.size)
        val rec = records[0]
        assertEquals(5000, rec.balance)
        assertEquals(200, rec.amount)
        assertEquals(ProcessType.ENTRY, rec.processType)
    }

    @Test
    fun `skips all-zero empty blocks`() {
        val emptyBlock = ByteArray(16)
        val records = useCase.invoke(testIdm, listOf(emptyBlock))
        assertEquals(0, records.size)
    }

    @Test
    fun `skips blocks with invalid date`() {
        val invalidDateBlock = ByteArray(16).apply {
            this[1] = 0x01
            this[2] = 0x30
            this[3] = 0x00
        }

        val records = useCase.invoke(testIdm, listOf(invalidDateBlock))

        assertEquals(0, records.size)
    }
}
