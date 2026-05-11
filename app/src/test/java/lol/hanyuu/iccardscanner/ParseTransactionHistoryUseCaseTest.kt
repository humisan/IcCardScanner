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
        // Date 2024/3/15:
        // year=24 (from 2000), month=3, day=15
        // dateWord = (24 shl 9) or (3 shl 5) or 15
        //          = 12288 or 96 or 15 = 12399 = 0x306F
        // byte[2] = 0x30, byte[3] = 0x6F
        val block = ByteArray(16).apply {
            this[1] = 0x01  // 運賃支払 -> ENTRY
            this[2] = 0x30
            this[3] = 0x6F
            // balance 5000 = 0x1388, LE: [10]=0x88, [11]=0x13
            this[10] = 0x88.toByte()
            this[11] = 0x13
            // amount 200 = 0x00C8, LE: [12]=0xC8, [13]=0x00
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
        val emptyBlock = ByteArray(16)  // all zeros
        val records = useCase.invoke(testIdm, listOf(emptyBlock))
        assertEquals(0, records.size)
    }
}
