package com.example.kaliumapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kaliumapp.data.database.dao.*
import com.example.kaliumapp.data.database.entities.*

@Database(
    entities = [
        User::class,
        WaterIntake::class,
        WaterReminder::class,
        DailyWaterSummary::class,
        FoodIntake::class,
        CalorieExpenditure::class,
        WalkSession::class,
        LocationPoint::class
    ],
    version = 4, // Increment version lagi
    exportSchema = false
)
abstract class KaliumDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun waterReminderDao(): WaterReminderDao
    abstract fun dailyWaterSummaryDao(): DailyWaterSummaryDao
    abstract fun foodIntakeDao(): FoodIntakeDao
    abstract fun calorieExpenditureDao(): CalorieExpenditureDao
    abstract fun walkSessionDao(): WalkSessionDao

    companion object {
        @Volatile
        private var INSTANCE: KaliumDatabase? = null

        fun getDatabase(context: Context): KaliumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaliumDatabase::class.java,
                    "kalium_database"
                )
                    .fallbackToDestructiveMigration() // Akan hapus dan buat ulang database
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Fungsi untuk clear semua data saat logout
        suspend fun clearAllData(context: Context) {
            val db = getDatabase(context)
            db.clearAllTables()
        }

        // Fungsi untuk reset database (hapus dan buat ulang)
        fun resetDatabase(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                context.deleteDatabase("kalium_database")
                INSTANCE = null
            }
        }
    }
}