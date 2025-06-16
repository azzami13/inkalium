package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.WaterIntake

@Dao
interface WaterIntakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntake(intake: WaterIntake): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleIntakes(intakes: List<WaterIntake>): List<Long>

    @Query("SELECT * FROM water_intake WHERE summary_id = :summaryId ORDER BY intake_time DESC")
    suspend fun getIntakesBySummaryId(summaryId: Int): List<WaterIntake>

    @Query("SELECT * FROM water_intake WHERE intake_id = :intakeId")
    suspend fun getIntakeById(intakeId: Int): WaterIntake?

    @Query("SELECT SUM(intake_ml) FROM water_intake WHERE summary_id = :summaryId")
    suspend fun getTotalIntake(summaryId: Int): Int?

    @Query("SELECT COUNT(*) FROM water_intake WHERE summary_id = :summaryId")
    suspend fun getIntakeCount(summaryId: Int): Int

    @Query("SELECT * FROM water_intake WHERE summary_id = :summaryId AND intake_time BETWEEN :startTime AND :endTime ORDER BY intake_time ASC")
    suspend fun getIntakesForTimeRange(summaryId: Int, startTime: Long, endTime: Long): List<WaterIntake>

    @Query("SELECT AVG(intake_ml) FROM water_intake WHERE summary_id = :summaryId")
    suspend fun getAverageIntake(summaryId: Int): Double?

    @Query("SELECT MAX(intake_ml) FROM water_intake WHERE summary_id = :summaryId")
    suspend fun getMaxIntake(summaryId: Int): Int?

    @Query("SELECT MIN(intake_ml) FROM water_intake WHERE summary_id = :summaryId")
    suspend fun getMinIntake(summaryId: Int): Int?

    @Update
    suspend fun updateIntake(intake: WaterIntake)

    @Query("UPDATE water_intake SET intake_ml = :intakeMl WHERE intake_id = :intakeId")
    suspend fun updateIntakeAmount(intakeId: Int, intakeMl: Int)

    @Query("DELETE FROM water_intake WHERE intake_id = :intakeId")
    suspend fun deleteIntake(intakeId: Int)

    @Query("DELETE FROM water_intake WHERE summary_id = :summaryId")
    suspend fun deleteIntakesBySummaryId(summaryId: Int)

    @Query("DELETE FROM water_intake")
    suspend fun clearAllIntakes()

    // Get recent intakes (last N intakes)
    @Query("SELECT * FROM water_intake WHERE summary_id = :summaryId ORDER BY intake_time DESC LIMIT :limit")
    suspend fun getRecentIntakes(summaryId: Int, limit: Int): List<WaterIntake>

    @Query("DELETE FROM water_intake")
    suspend fun deleteAllIntakes()

    // Get intakes for today (useful for charts)
    @Query("""
        SELECT * FROM water_intake 
        WHERE summary_id = :summaryId 
        AND intake_time >= :startOfDay 
        AND intake_time <= :endOfDay 
        ORDER BY intake_time ASC
    """)
    suspend fun getTodayIntakes(summaryId: Int, startOfDay: Long, endOfDay: Long): List<WaterIntake>
}