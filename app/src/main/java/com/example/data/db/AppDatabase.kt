package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.FinanceDao
import com.example.data.entity.Account
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.DetectedMessage
import com.example.data.entity.Investment
import com.example.data.entity.Loan
import com.example.data.entity.Reminder
import com.example.data.entity.SavingsGoal
import com.example.data.entity.Transaction

@Database(
    entities = [
        Account::class,
        Transaction::class,
        Category::class,
        SavingsGoal::class,
        Investment::class,
        Loan::class,
        Reminder::class,
        DetectedMessage::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
