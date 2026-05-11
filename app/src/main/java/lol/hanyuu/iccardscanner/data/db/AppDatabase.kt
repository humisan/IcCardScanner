package lol.hanyuu.iccardscanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import lol.hanyuu.iccardscanner.data.db.converter.Converters
import lol.hanyuu.iccardscanner.data.db.dao.CardDao
import lol.hanyuu.iccardscanner.data.db.dao.ScanRecordDao
import lol.hanyuu.iccardscanner.data.db.dao.TransactionRecordDao
import lol.hanyuu.iccardscanner.data.db.entity.CardEntity
import lol.hanyuu.iccardscanner.data.db.entity.ScanRecordEntity
import lol.hanyuu.iccardscanner.data.db.entity.TransactionRecordEntity

@Database(
    entities = [CardEntity::class, ScanRecordEntity::class, TransactionRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun scanRecordDao(): ScanRecordDao
    abstract fun transactionRecordDao(): TransactionRecordDao
}
