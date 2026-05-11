package lol.hanyuu.iccardscanner.di

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, AppDatabase::class.java, "ic_card_scanner.db").build()

    @Provides fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()
    @Provides fun provideScanRecordDao(db: AppDatabase): ScanRecordDao = db.scanRecordDao()
    @Provides fun provideTransactionRecordDao(db: AppDatabase): TransactionRecordDao = db.transactionRecordDao()
}
