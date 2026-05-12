package lol.hanyuu.iccardscanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import lol.hanyuu.iccardscanner.data.db.AppDatabase
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity
import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.domain.model.ProcessType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionRecordDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun recentRecordsKeepFelicaReadOrderForSameDateTrips() = runBlocking {
        val cardIdm = "AABBCCDD11223344"
        val sameDate = 1_778_515_200_000L
        db.cardDao().upsert(CardEntity(cardIdm, CardType.ICOCA, 0x0003, "ICOCA", 118, sameDate))

        db.transactionRecordDao().replaceByCard(
            cardIdm,
            listOf(
                record(cardIdm, sameDate, balance = 118, entryStationCode = 0x8A9C, exitStationCode = 0x8BB0),
                record(cardIdm, sameDate, balance = 378, entryStationCode = 0x8BB0, exitStationCode = 0x8A9C),
                record(cardIdm, sameDate - 86_400_000L, balance = 638, entryStationCode = 0x8A9C, exitStationCode = 0x8BB0)
            )
        )

        val recent = db.transactionRecordDao().getRecentByCard(cardIdm, 3).first()

        assertEquals(3, recent.size)
        assertEquals(0x8A9C, recent[0].entryStationCode)
        assertEquals(0x8BB0, recent[0].exitStationCode)
        assertEquals(0x8BB0, recent[1].entryStationCode)
        assertEquals(0x8A9C, recent[1].exitStationCode)
    }

    private fun record(
        cardIdm: String,
        transactionDate: Long,
        balance: Int,
        entryStationCode: Int,
        exitStationCode: Int
    ): TransactionRecordEntity =
        TransactionRecordEntity(
            cardIdm = cardIdm,
            transactionDate = transactionDate,
            processType = ProcessType.EXIT,
            amount = 260,
            balance = balance,
            entryStationCode = entryStationCode,
            exitStationCode = exitStationCode,
            details = "area=a0;terminal=改札機;process=0x01;sequence=256"
        )
}
