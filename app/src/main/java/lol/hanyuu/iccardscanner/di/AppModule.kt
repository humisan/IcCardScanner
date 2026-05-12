package lol.hanyuu.iccardscanner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lol.hanyuu.iccardscanner.data.db.AppDatabase
import lol.hanyuu.iccardscanner.data.db.dao.CardDao
import lol.hanyuu.iccardscanner.data.db.dao.ScanRecordDao
import lol.hanyuu.iccardscanner.data.db.dao.TransactionRecordDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ic_card_scanner.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()
    @Provides fun provideScanRecordDao(db: AppDatabase): ScanRecordDao = db.scanRecordDao()
    @Provides fun provideTransactionRecordDao(db: AppDatabase): TransactionRecordDao = db.transactionRecordDao()

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transaction_records ADD COLUMN sequence INTEGER NOT NULL DEFAULT 0")
            db.execSQL("DROP INDEX IF EXISTS index_transaction_records_full_history_key")
            // Remove rows that would conflict on the new unique key (keep the highest id per group)
            db.execSQL(
                """
                DELETE FROM transaction_records WHERE id NOT IN (
                    SELECT MAX(id) FROM transaction_records
                    GROUP BY cardIdm, transactionDate, processType, balance,
                             entryStationCode, exitStationCode, sequence
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_transaction_records_history_key
                ON transaction_records(
                    cardIdm, transactionDate, processType, balance,
                    entryStationCode, exitStationCode, sequence
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS index_transaction_records_cardIdm_transactionDate_processType_amount")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_transaction_records_full_history_key
                ON transaction_records(
                    cardIdm,
                    transactionDate,
                    processType,
                    amount,
                    balance,
                    entryStationCode,
                    exitStationCode
                )
                """.trimIndent()
            )
        }
    }
}
