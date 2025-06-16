package com.example.kaliumapp.data.repository

import android.util.Log
import com.example.kaliumapp.data.database.dao.*
import com.example.kaliumapp.data.database.entities.*
import com.example.kaliumapp.model.UserResponse
import com.example.kaliumapp.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val userDao: UserDao,
    private val dailyWaterSummaryDao: DailyWaterSummaryDao,
    private val waterIntakeDao: WaterIntakeDao,
    private val waterReminderDao: WaterReminderDao,
    private val apiService: ApiService
) {

    // Primary storage operations (Room)
    suspend fun insertWaterSummary(summary: DailyWaterSummary): Long {
        return try {
            // Pastikan user exists dulu
            val user = userDao.getUserById(summary.userId)
            if (user == null) {
                throw Exception("User with ID ${summary.userId} not found. Cannot create water summary.")
            }

            val insertedId = dailyWaterSummaryDao.insertSummary(summary)
            Log.d("WaterRepository", "Water summary saved to Room with ID: $insertedId")
            insertedId
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error inserting water summary", e)
            throw e
        }
    }

    suspend fun getWaterSummary(userId: Int, date: String): DailyWaterSummary? {
        return try {
            val summary = dailyWaterSummaryDao.getSummaryByDate(userId, date)
            Log.d("WaterRepository", "Retrieved water summary for user $userId, date $date: $summary")
            summary
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting water summary", e)
            null
        }
    }

    suspend fun getWaterSummaryById(summaryId: Int): DailyWaterSummary? {
        return try {
            val summary = dailyWaterSummaryDao.getSummaryById(summaryId)
            Log.d("WaterRepository", "Retrieved water summary by ID $summaryId: $summary")
            summary
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting water summary by ID", e)
            null
        }
    }

    suspend fun updateWaterSummary(summary: DailyWaterSummary) {
        try {
            dailyWaterSummaryDao.updateSummary(summary)
            Log.d("WaterRepository", "Water summary updated: ${summary.summaryId}")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error updating water summary", e)
            throw e
        }
    }

    suspend fun insertWaterIntake(intake: WaterIntake) {
        try {
            // Pastikan summary exists dulu
            val summary = dailyWaterSummaryDao.getSummaryById(intake.summaryId)
            if (summary == null) {
                throw Exception("Summary with ID ${intake.summaryId} not found. Cannot insert water intake.")
            }

            val insertedId = waterIntakeDao.insertIntake(intake)
            Log.d("WaterRepository", "Water intake saved to Room: ${intake.intakeMl}ml with ID: $insertedId")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error inserting water intake", e)
            throw e
        }
    }

    suspend fun getTotalWaterIntake(summaryId: Int): Int {
        return try {
            val total = waterIntakeDao.getTotalIntake(summaryId) ?: 0
            Log.d("WaterRepository", "Total water intake for summary $summaryId: $total ml")
            total
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting total water intake", e)
            0
        }
    }

    suspend fun getWaterIntakesBySummary(summaryId: Int): List<WaterIntake> {
        return try {
            val intakes = waterIntakeDao.getIntakesBySummaryId(summaryId)
            Log.d("WaterRepository", "Retrieved ${intakes.size} water intakes for summary $summaryId")
            intakes
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting water intakes by summary", e)
            emptyList()
        }
    }

    suspend fun insertWaterReminder(reminder: WaterReminder) {
        try {
            // Pastikan summary exists dulu
            val summary = dailyWaterSummaryDao.getSummaryById(reminder.summaryId)
            if (summary == null) {
                throw Exception("Summary with ID ${reminder.summaryId} not found. Cannot insert water reminder.")
            }

            waterReminderDao.insertReminder(reminder)
            Log.d("WaterRepository", "Water reminder saved to Room for summary ${reminder.summaryId}")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error inserting water reminder", e)
            throw e
        }
    }

    suspend fun getWaterReminders(summaryId: Int): List<WaterReminder> {
        return try {
            val reminders = waterReminderDao.getReminders(summaryId)
            Log.d("WaterRepository", "Retrieved ${reminders.size} reminders for summary $summaryId")
            reminders
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting water reminders", e)
            emptyList()
        }
    }

    suspend fun markReminderAsDone(reminderId: Int) {
        try {
            waterReminderDao.markReminderAsDone(reminderId)
            Log.d("WaterRepository", "Reminder $reminderId marked as done")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error marking reminder as done", e)
        }
    }

    suspend fun getAllUserWaterSummaries(userId: Int): List<DailyWaterSummary> {
        return try {
            val summaries = dailyWaterSummaryDao.getAllSummariesByUser(userId)
            Log.d("WaterRepository", "Retrieved ${summaries.size} water summaries for user $userId")
            summaries
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting all user water summaries", e)
            emptyList()
        }
    }

    suspend fun clearUserWaterData(userId: Int) {
        try {
            dailyWaterSummaryDao.deleteAllUserSummaries(userId)
            Log.d("WaterRepository", "User water data cleared from Room for user $userId")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error clearing user water data", e)
        }
    }

    // User operations
    suspend fun insertUser(user: User) {
        try {
            userDao.insertUser(user)
            Log.d("WaterRepository", "User inserted successfully: ${user.email}")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error inserting user", e)
            throw e
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            userDao.getUserByEmail(email)
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting user by email", e)
            null
        }
    }

    suspend fun getUserById(userId: Int): User? {
        return try {
            val user = userDao.getUserById(userId)
            Log.d("WaterRepository", "Retrieved user by ID $userId: ${user?.email}")
            user
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error getting user by ID", e)
            null
        }
    }

    suspend fun updateLoginStats(id: Int, lastLogin: Long) {
        try {
            userDao.updateLoginStats(id, lastLogin)
            Log.d("WaterRepository", "Updated login stats for user $id")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error updating login stats", e)
        }
    }

    suspend fun updateUser(user: User) {
        try {
            userDao.updateUser(user)
            Log.d("WaterRepository", "User updated: ${user.id}")
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error updating user", e)
            throw e
        }
    }

    // API sync operations (untuk nanti)
    suspend fun syncDataToServer(user: User) {
        val userResponse = UserResponse(
            id = user.id,
            email = user.email,
            username = user.username,
            umur = user.umur,
            jenisKelamin = user.jenisKelamin,
            beratBadan = user.beratBadan,
            tinggiBadan = user.tinggiBadan,
            profileImage = user.profileImage
        )
        try {
            val response = apiService.updateUserProfile("Bearer ${user.id}", userResponse)
            if (response.isSuccessful) {
                Log.d("WaterRepository", "Data synced to server successfully")
            }
        } catch (e: Exception) {
            Log.e("WaterRepository", "Error syncing to server", e)
        }
    }
}