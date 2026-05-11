package lol.hanyuu.iccardscanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import lol.hanyuu.iccardscanner.data.db.AppDatabase
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity
import lol.hanyuu.iccardscanner.domain.model.CardType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardDaoTest {
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
    fun upsertAndQueryCard() = runBlocking {
        val card = CardEntity("AABBCCDD11223344", CardType.SUICA, 0x0003, "Suica", 5000, 1000L)
        db.cardDao().upsert(card)
        val result = db.cardDao().getAllCards().first()
        assertEquals(1, result.size)
        assertEquals(5000, result[0].lastBalance)
    }

    @Test
    fun upsertUpdatesExistingCard() = runBlocking {
        val card = CardEntity("AABBCCDD11223344", CardType.SUICA, 0x0003, "Suica", 5000, 1000L)
        db.cardDao().upsert(card)
        db.cardDao().upsert(card.copy(lastBalance = 4800))
        val result = db.cardDao().getAllCards().first()
        assertEquals(1, result.size)
        assertEquals(4800, result[0].lastBalance)
    }

    @Test
    fun getCardByIdm() = runBlocking {
        val card = CardEntity("AABBCCDD11223344", CardType.SUICA, 0x0003, "Suica", 5000, 1000L)
        db.cardDao().upsert(card)
        val result = db.cardDao().getCardByIdm("AABBCCDD11223344")
        assertEquals("Suica", result?.nickname)
        assertNull(db.cardDao().getCardByIdm("NONEXISTENT"))
    }
}
