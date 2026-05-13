package lol.hanyuu.iccardscanner.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity

@Dao
interface TransactionRecordDao {
    @Query("SELECT * FROM transaction_records WHERE cardIdm = :idm ORDER BY transactionDate DESC, id ASC")
    fun getByCard(idm: String): Flow<List<TransactionRecordEntity>>

    @Query("SELECT * FROM transaction_records WHERE cardIdm = :idm ORDER BY transactionDate DESC, id ASC LIMIT :limit")
    fun getRecentByCard(idm: String, limit: Int): Flow<List<TransactionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(records: List<TransactionRecordEntity>)

    @Query("DELETE FROM transaction_records WHERE cardIdm = :idm")
    suspend fun deleteByCard(idm: String)
}
