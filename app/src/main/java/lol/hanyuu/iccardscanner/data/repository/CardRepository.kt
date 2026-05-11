package lol.hanyuu.iccardscanner.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lol.hanyuu.iccardscanner.data.db.dao.CardDao
import lol.hanyuu.iccardscanner.data.db.dao.ScanRecordDao
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity
import lol.hanyuu.iccardscanner.data.db.entity.ScanRecordEntity
import lol.hanyuu.iccardscanner.domain.model.Card
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val scanRecordDao: ScanRecordDao
) {
    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getCard(idm: String): Card? = cardDao.getCardByIdm(idm)?.toDomain()

    suspend fun upsertCard(entity: CardEntity) = cardDao.upsert(entity)

    suspend fun insertScanRecord(entity: ScanRecordEntity) = scanRecordDao.insert(entity)

    suspend fun deleteCard(idm: String) = cardDao.deleteByIdm(idm)

    suspend fun updateNickname(idm: String, nickname: String) = cardDao.updateNickname(idm, nickname)

    private fun CardEntity.toDomain() = Card(
        idm = idm,
        type = type,
        nickname = nickname,
        lastBalance = lastBalance,
        lastScannedAt = lastScannedAt
    )
}
