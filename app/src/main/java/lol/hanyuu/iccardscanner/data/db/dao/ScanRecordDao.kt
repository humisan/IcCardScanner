package lol.hanyuu.iccardscanner.data.db.dao

import androidx.room.*
import lol.hanyuu.iccardscanner.data.db.entity.ScanRecordEntity

@Dao
interface ScanRecordDao {
    @Insert
    suspend fun insert(record: ScanRecordEntity)
}
