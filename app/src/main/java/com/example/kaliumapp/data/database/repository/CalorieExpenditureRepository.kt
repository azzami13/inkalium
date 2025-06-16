package com.example.kaliumapp.data.repository

import android.util.Log
import com.example.kaliumapp.data.database.dao.CalorieExpenditureDao
import com.example.kaliumapp.data.database.dao.UserDao
import com.example.kaliumapp.data.database.entities.CalorieExpenditure
import com.example.kaliumapp.remote.ApiService
import com.example.kaliumapp.model.CalorieExpenditureRequest
import com.example.kaliumapp.model.CalorieExpenditureResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalorieExpenditureRepository @Inject constructor(
    private val userDao: UserDao,
    private val calorieExpenditureDao: CalorieExpenditureDao,
    private val apiService: ApiService
) {

    // Primary storage operations (Room)
    suspend fun insertCalorieExpenditure(expenditure: CalorieExpenditure) {
        calorieExpenditureDao.insertCalorieExpenditure(expenditure)
        Log.d("CalorieExpenditureRepository", "Calorie expenditure saved to Room: ${expenditure.calories}")
    }

    suspend fun getCalorieExpenditureHistory(userId: Int): List<CalorieExpenditure> {
        return calorieExpenditureDao.getCalorieExpenditureHistory(userId)
    }

    suspend fun getCalorieExpenditureByDate(userId: Int, date: String): List<CalorieExpenditure> {
        return calorieExpenditureDao.getCalorieExpenditureByDate(userId, date)
    }

    suspend fun clearUserCalorieData(userId: Int) {
        calorieExpenditureDao.deleteAllUserCalorieExpenditure(userId)
    }

    suspend fun calculateCaloriesBurned(steps: Float, userId: Int): Float {
        val user = userDao.getUserById(userId) ?: run {
            Log.e("CalorieExpenditureRepository", "User not found for userId=$userId")
            return 0f
        }
        val strideLength = 0.78f
        val distanceKm = (steps * strideLength) / 1000
        val met = 3.5f
        val weightKg = user.beratBadan.toFloat()
        val caloriesBurned = met * weightKg * (distanceKm / 5)

        // Simpan ke Room
        val expenditure = CalorieExpenditure(
            userId = userId,
            calories = caloriesBurned,
            date = java.time.LocalDate.now().toString()
        )
        insertCalorieExpenditure(expenditure)

        Log.d("CalorieExpenditureRepository", "Calories calculated and saved: $caloriesBurned")
        return caloriesBurned
    }

    // API operations (untuk sync nanti)
    suspend fun saveCalorieExpenditure(userId: Int, calories: Float, date: String, token: String) {
        val request = CalorieExpenditureRequest(userId, calories, date)
        Log.d("CalorieExpenditureRepository", "Syncing to API: $request")
        try {
            val response = apiService.saveCalorieExpenditure("Bearer $token", request)
            if (response.isSuccessful) {
                Log.d("CalorieExpenditureRepository", "API sync successful: ${response.body()}")
            } else {
                Log.e("CalorieExpenditureRepository", "API sync failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CalorieExpenditureRepository", "API sync error: ${e.message}", e)
        }
    }

    suspend fun getCalorieExpenditureHistoryFromApi(userId: Int, token: String): List<CalorieExpenditureResponse> {
        Log.d("CalorieExpenditureRepository", "Fetching history from API for userId=$userId")
        return try {
            val response = apiService.getCalorieExpenditureHistory("Bearer $token", userId)
            if (response.isSuccessful) {
                val history = response.body() ?: emptyList()
                Log.d("CalorieExpenditureRepository", "API history fetched: ${history.size} entries")
                history
            } else {
                Log.e("CalorieExpenditureRepository", "API history fetch failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("CalorieExpenditureRepository", "API history fetch error: ${e.message}", e)
            emptyList()
        }
    }
}