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
            this[4] = 0x30
            this[5] = 0x6F
            this[10] = 0x88.toByte()
            this[11] = 0x13
        }

        val records = useCase.invoke(testIdm, listOf(block))

        assertEquals(1, records.size)
        val rec = records[0]
        assertEquals(5000, rec.balance)
        assertEquals(0, rec.amount)
        assertEquals(ProcessType.ENTRY, rec.processType)
    }

    @Test
    fun `computes amount from next older balance`() {
        val newest = ByteArray(16).apply {
            this[1] = 0x02
            this[4] = 0x30
            this[5] = 0x70
            this[10] = 0x20
            this[11] = 0x03
        }
        val older = ByteArray(16).apply {
            this[1] = 0x01
            this[4] = 0x30
            this[5] = 0x6F
            this[10] = 0xE8.toByte()
            this[11] = 0x03
        }

        val records = useCase.invoke(testIdm, listOf(newest, older))

        assertEquals(2, records.size)
        assertEquals(200, records[0].amount)
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
            this[4] = 0x30
            this[5] = 0x00
        }

        val records = useCase.invoke(testIdm, listOf(invalidDateBlock))

        assertEquals(0, records.size)
    }
}
