package com.example.kaliumapp.di

import android.content.Context
import androidx.room.Room
import com.example.kaliumapp.data.database.KaliumDatabase
import com.example.kaliumapp.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KaliumDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            KaliumDatabase::class.java,
            "kalium_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: KaliumDatabase): UserDao = database.userDao()

    @Provides
    fun provideFoodIntakeDao(database: KaliumDatabase): FoodIntakeDao = database.foodIntakeDao()

    @Provides
    fun provideWaterIntakeDao(database: KaliumDatabase): WaterIntakeDao = database.waterIntakeDao()

    @Provides
    fun provideDailyWaterSummaryDao(database: KaliumDatabase): DailyWaterSummaryDao = database.dailyWaterSummaryDao()

    @Provides
    fun provideWaterReminderDao(database: KaliumDatabase): WaterReminderDao = database.waterReminderDao()

    @Provides
    fun provideCalorieExpenditureDao(database: KaliumDatabase): CalorieExpenditureDao = database.calorieExpenditureDao()

    @Provides
    fun provideWalkSessionDao(database: KaliumDatabase): WalkSessionDao = database.walkSessionDao()
}