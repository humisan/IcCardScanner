package lol.hanyuu.iccardscanner.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lol.hanyuu.iccardscanner.data.db.dao.TransactionRecordDao
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val transactionRecordDao: TransactionRecordDao
) {
    fun getTransactions(cardIdm: String): Flow<List<TransactionRecord>> =
        transactionRecordDao.getByCard(cardIdm).map { list -> list.map { it.toDomain() } }

    fun getRecentTransactions(cardIdm: String, limit: Int): Flow<List<TransactionRecord>> =
        transactionRecordDao.getRecentByCard(cardIdm, limit).map { list -> list.map { it.toDomain() } }

    suspend fun insertTransactionsIgnoreConflicts(records: List<TransactionRecordEntity>) =
        transactionRecordDao.insertIgnoreConflict(records)

    suspend fun replaceTransactions(cardIdm: String, records: List<TransactionRecordEntity>) =
        transactionRecordDao.replaceByCard(cardIdm, records)

    private fun TransactionRecordEntity.toDomain() = TransactionRecord(
        id = id,
        cardIdm = cardIdm,
        transactionDate = transactionDate,
        processType = processType,
        amount = amount,
        balance = balance,
        entryStationCode = entryStationCode,
        exitStationCode = exitStationCode,
        details = details
    )
}
