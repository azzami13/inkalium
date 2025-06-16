package com.example.kaliumapp.di

import android.content.Context
import com.example.kaliumapp.remote.ApiService
import com.example.kaliumapp.data.repository.*
import com.example.kaliumapp.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        apiService: ApiService,
        @ApplicationContext context: Context
    ): UserRepository {
        return UserRepository(userDao, apiService, context)
    }

    @Provides
    @Singleton
    fun provideCalorieRepository(
        userDao: UserDao
    ): CalorieRepository {
        return CalorieRepository(userDao)
    }

    @Provides
    @Singleton
    fun provideCalorieExpenditureRepository(
        userDao: UserDao,
        calorieExpenditureDao: CalorieExpenditureDao,
        apiService: ApiService
    ): CalorieExpenditureRepository {
        return CalorieExpenditureRepository(userDao, calorieExpenditureDao, apiService)
    }

    @Provides
    @Singleton
    fun provideFoodRepository(
        apiService: ApiService,
        foodIntakeDao: FoodIntakeDao,
        userDao: UserDao
    ): FoodRepository {
        return FoodRepository(apiService, foodIntakeDao, userDao)
    }

    @Provides
    @Singleton
    fun provideWaterRepository(
        userDao: UserDao,
        dailyWaterSummaryDao: DailyWaterSummaryDao,
        waterIntakeDao: WaterIntakeDao,
        waterReminderDao: WaterReminderDao,
        apiService: ApiService
    ): WaterRepository {
        return WaterRepository(userDao, dailyWaterSummaryDao, waterIntakeDao, waterReminderDao, apiService)
    }

    @Provides
    @Singleton
    fun provideTrackingRepository(
        walkSessionDao: WalkSessionDao
    ): TrackingRepository {
        return TrackingRepository(walkSessionDao)
    }
}