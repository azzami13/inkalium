package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.DailyWaterSummary

@Dao
interface DailyWaterSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DailyWaterSummary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleSummaries(summaries: List<DailyWaterSummary>): List<Long>

    @Query("SELECT * FROM daily_water_summary WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getSummaryByDate(userId: Int, date: String): DailyWaterSummary?

    @Query("SELECT * FROM daily_water_summary WHERE summary_id = :summaryId")
    suspend fun getSummaryById(summaryId: Int): DailyWaterSummary?

    @Query("SELECT * FROM daily_water_summary WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllSummariesByUser(userId: Int): List<DailyWaterSummary>

    @Query("SELECT * FROM daily_water_summary WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummaries(userId: Int, limit: Int): List<DailyWaterSummary>

    @Query("SELECT * FROM daily_water_summary WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getSummariesForDateRange(userId: Int, startDate: String, endDate: String): List<DailyWaterSummary>

    @Query("SELECT COUNT(*) FROM daily_water_summary WHERE userId = :userId AND status = 'Completed'")
    suspend fun getCompletedDaysCount(userId: Int): Int

    @Query("SELECT AVG(totalIntakeMl) FROM daily_water_summary WHERE userId = :userId AND totalIntakeMl > 0")
    suspend fun getAverageWaterIntake(userId: Int): Double?

    @Update
    suspend fun updateSummary(summary: DailyWaterSummary)

    @Query("UPDATE daily_water_summary SET totalIntakeMl = :totalIntake, remainingMl = :remaining, status = :status WHERE summary_id = :summaryId")
    suspend fun updateSummaryProgress(summaryId: Int, totalIntake: Int, remaining: Int, status: String)

    @Query("UPDATE daily_water_summary SET status = :status WHERE summary_id = :summaryId")
    suspend fun updateSummaryStatus(summaryId: Int, status: String)

    @Delete
    suspend fun deleteSummary(summary: DailyWaterSummary)

    @Query("DELETE FROM daily_water_summary WHERE summary_id = :summaryId")
    suspend fun deleteSummaryById(summaryId: Int)

    @Query("DELETE FROM daily_water_summary WHERE userId = :userId")
    suspend fun deleteAllUserSummaries(userId: Int)

    @Query("DELETE FROM daily_water_summary WHERE userId = :userId AND date < :beforeDate")
    suspend fun deleteOldSummaries(userId: Int, beforeDate: String)

    @Query("DELETE FROM daily_water_summary")
    suspend fun clearAllSummaries()

    @Query("DELETE FROM daily_water_summary")
    suspend fun deleteAllSummaries()

    @Query("""
        SELECT * FROM daily_water_summary 
        WHERE userId = :userId 
        AND totalIntakeMl >= remainingMl + totalIntakeMl * 0.8
        ORDER BY date DESC
    """)
    suspend fun getSuccessfulDays(userId: Int): List<DailyWaterSummary>

    @Query("SELECT * FROM daily_water_summary WHERE userId = :userId AND status = :status ORDER BY date DESC")
    suspend fun getSummariesByStatus(userId: Int, status: String): List<DailyWaterSummary>
}