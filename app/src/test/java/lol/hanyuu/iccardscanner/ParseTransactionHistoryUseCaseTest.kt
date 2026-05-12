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
        assertEquals(ProcessType.EXIT, rec.processType)
    }

    @Test
    fun `computes amount from next older balance`() {
        val newest = ByteArray(16).apply {
            this[1] = 0x01
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
        assertEquals(ProcessType.EXIT, records[0].processType)
    }

    @Test
    fun `keeps same-day same-fare reverse trips as separate records`() {
        val newest = ByteArray(16).apply {
            this[1] = 0x01
            this[4] = 0x34
            this[5] = 0xAC.toByte()
            this[6] = 0x8A.toByte()
            this[7] = 0x9C.toByte()
            this[8] = 0x8B.toByte()
            this[9] = 0xB0.toByte()
            this[10] = 0x76
            this[11] = 0x00
        }
        val olderReverseTrip = ByteArray(16).apply {
            this[1] = 0x01
            this[4] = 0x34
            this[5] = 0xAC.toByte()
            this[6] = 0x8B.toByte()
            this[7] = 0xB0.toByte()
            this[8] = 0x8A.toByte()
            this[9] = 0x9C.toByte()
            this[10] = 0x7A
            this[11] = 0x01
        }
        val oldest = ByteArray(16).apply {
            this[1] = 0x01
            this[4] = 0x34
            this[5] = 0xAB.toByte()
            this[10] = 0x7E
            this[11] = 0x02
        }

        val records = useCase.invoke(testIdm, listOf(newest, olderReverseTrip, oldest))

        assertEquals(3, records.size)
        assertEquals(260, records[0].amount)
        assertEquals(260, records[1].amount)
        assertEquals(0x8A9C, records[0].entryStationCode)
        assertEquals(0x8BB0, records[0].exitStationCode)
        assertEquals(0x8BB0, records[1].entryStationCode)
        assertEquals(0x8A9C, records[1].exitStationCode)
    }

    @Test
    fun `process code 0x02 is charge`() {
        val block = ByteArray(16).apply {
            this[1] = 0x02
            this[4] = 0x30
            this[5] = 0x6F
        }

        val records = useCase.invoke(testIdm, listOf(block))

        assertEquals(1, records.size)
        assertEquals(ProcessType.CHARGE, records[0].processType)
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
