package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.WaterReminder

@Dao
interface WaterReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: WaterReminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultipleReminders(reminders: List<WaterReminder>): List<Long>

    @Query("SELECT * FROM water_reminder WHERE summary_id = :summaryId ORDER BY reminder_time ASC")
    suspend fun getReminders(summaryId: Int): List<WaterReminder>

    @Query("SELECT * FROM water_reminder WHERE reminder_id = :reminderId")
    suspend fun getReminderById(reminderId: Int): WaterReminder?

    @Query("SELECT * FROM water_reminder WHERE summary_id = :summaryId AND status = :status ORDER BY reminder_time ASC")
    suspend fun getRemindersByStatus(summaryId: Int, status: String): List<WaterReminder>

    @Query("SELECT * FROM water_reminder WHERE summary_id = :summaryId AND status = 'Pending' ORDER BY reminder_time ASC")
    suspend fun getPendingReminders(summaryId: Int): List<WaterReminder>

    @Query("SELECT * FROM water_reminder WHERE summary_id = :summaryId AND status = 'Done' ORDER BY reminder_time ASC")
    suspend fun getCompletedReminders(summaryId: Int): List<WaterReminder>

    @Query("SELECT COUNT(*) FROM water_reminder WHERE summary_id = :summaryId")
    suspend fun getReminderCount(summaryId: Int): Int

    @Query("SELECT COUNT(*) FROM water_reminder WHERE summary_id = :summaryId AND status = 'Done'")
    suspend fun getCompletedReminderCount(summaryId: Int): Int

    @Query("SELECT COUNT(*) FROM water_reminder WHERE summary_id = :summaryId AND status = 'Pending'")
    suspend fun getPendingReminderCount(summaryId: Int): Int

    @Update
    suspend fun updateReminder(reminder: WaterReminder)

    @Query("UPDATE water_reminder SET status = 'Done' WHERE reminder_id = :reminderId")
    suspend fun markReminderAsDone(reminderId: Int)

    @Query("UPDATE water_reminder SET status = 'Skipped' WHERE reminder_id = :reminderId")
    suspend fun markReminderAsSkipped(reminderId: Int)

    @Query("UPDATE water_reminder SET status = :status WHERE reminder_id = :reminderId")
    suspend fun updateReminderStatus(reminderId: Int, status: String)

    @Query("UPDATE water_reminder SET reminder_time = :newTime WHERE reminder_id = :reminderId")
    suspend fun updateReminderTime(reminderId: Int, newTime: Long)

    @Delete
    suspend fun deleteReminder(reminder: WaterReminder)

    @Query("DELETE FROM water_reminder WHERE reminder_id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)

    @Query("DELETE FROM water_reminder WHERE summary_id = :summaryId")
    suspend fun deleteRemindersBySummaryId(summaryId: Int)

    @Query("DELETE FROM water_reminder WHERE summary_id = :summaryId AND status = :status")
    suspend fun deleteRemindersByStatus(summaryId: Int, status: String)

    @Query("DELETE FROM water_reminder")
    suspend fun clearAllReminders()

    @Query("""
        SELECT * FROM water_reminder 
        WHERE summary_id = :summaryId 
        AND reminder_time BETWEEN :startTime AND :endTime 
        ORDER BY reminder_time ASC
    """)
    suspend fun getRemindersForTimeRange(summaryId: Int, startTime: Long, endTime: Long): List<WaterReminder>

    @Query("""
        SELECT * FROM water_reminder 
        WHERE summary_id = :summaryId 
        AND reminder_time <= :currentTime 
        AND status = 'Pending'
        ORDER BY reminder_time ASC
    """)
    suspend fun getOverdueReminders(summaryId: Int, currentTime: Long): List<WaterReminder>

    @Query("DELETE FROM water_reminder")
    suspend fun deleteAllReminders()

    @Query("""
        SELECT * FROM water_reminder 
        WHERE summary_id = :summaryId 
        AND reminder_time > :currentTime 
        AND status = 'Pending'
        ORDER BY reminder_time ASC 
        LIMIT 1
    """)
    suspend fun getNextReminder(summaryId: Int, currentTime: Long): WaterReminder?
}