package lol.hanyuu.iccardscanner.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY lastScannedAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE idm = :idm LIMIT 1")
    suspend fun getCardByIdm(idm: String): CardEntity?

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Query("DELETE FROM cards WHERE idm = :idm")
    suspend fun deleteByIdm(idm: String)

    @Query("UPDATE cards SET nickname = :nickname WHERE idm = :idm")
    suspend fun updateNickname(idm: String, nickname: String)
}
