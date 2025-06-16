package com.example.kaliumapp.data.repository

import android.util.Log
import com.example.kaliumapp.data.database.dao.FoodIntakeDao
import com.example.kaliumapp.data.database.dao.UserDao
import com.example.kaliumapp.data.database.entities.FoodIntake
import com.example.kaliumapp.model.FoodIntakeRequest
import com.example.kaliumapp.model.FoodItem
import com.example.kaliumapp.remote.ApiService
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val apiService: ApiService,
    private val foodIntakeDao: FoodIntakeDao,
    private val userDao: UserDao // Tambahkan UserDao untuk validasi
) {

    // Primary storage operations (Room)
    suspend fun insertFoodIntake(foodIntake: FoodIntake): Long {
        return try {
            // Pastikan user exists dulu sebelum insert
            val user = userDao.getUserById(foodIntake.userId)
            if (user == null) {
                throw Exception("User with ID ${foodIntake.userId} not found. Cannot insert food intake.")
            }

            val insertedId = foodIntakeDao.insert(foodIntake)
            Log.d("FoodRepository", "Food intake saved to Room: ${foodIntake.foodName} with ID: $insertedId")
            insertedId
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error inserting food intake", e)
            throw e
        }
    }

    suspend fun getDailyFoodIntakes(userId: Int, date: LocalDate): List<FoodIntake> {
        return try {
            // Pastikan user exists
            val userExists = checkUserExists(userId)
            if (!userExists) {
                Log.w("FoodRepository", "User $userId does not exist")
                return emptyList()
            }

            val startTime = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val endTime = date.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val intakes = foodIntakeDao.getFoodIntakesForDay(userId, startTime, endTime)
            Log.d("FoodRepository", "Retrieved ${intakes.size} food intakes for $date: $intakes")
            intakes
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error getting daily food intakes", e)
            emptyList()
        }
    }

    suspend fun getAllFoodIntakes(userId: Int): List<FoodIntake> {
        return try {
            // Pastikan user exists
            val userExists = checkUserExists(userId)
            if (!userExists) {
                Log.w("FoodRepository", "User $userId does not exist")
                return emptyList()
            }

            val intakes = foodIntakeDao.getAllFoodIntakes(userId)
            Log.d("FoodRepository", "Retrieved ${intakes.size} total food intakes for user $userId")
            intakes
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error getting all food intakes", e)
            emptyList()
        }
    }

    suspend fun updateFoodIntake(foodIntake: FoodIntake) {
        try {
            // Pastikan user exists
            val user = userDao.getUserById(foodIntake.userId)
            if (user == null) {
                throw Exception("User with ID ${foodIntake.userId} not found. Cannot update food intake.")
            }

            foodIntakeDao.updateFoodIntake(foodIntake)
            Log.d("FoodRepository", "Food intake updated: ${foodIntake.id}")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error updating food intake", e)
            throw e
        }
    }

    suspend fun deleteFoodIntake(foodIntakeId: Int) {
        try {
            foodIntakeDao.deleteFoodIntake(foodIntakeId)
            Log.d("FoodRepository", "Food intake deleted: $foodIntakeId")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error deleting food intake", e)
            throw e
        }
    }

    suspend fun getFoodIntakeById(foodIntakeId: Int): FoodIntake? {
        return try {
            foodIntakeDao.getFoodIntakeById(foodIntakeId)
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error getting food intake by ID", e)
            null
        }
    }

    suspend fun clearUserFoodData(userId: Int) {
        try {
            foodIntakeDao.deleteAllUserFoodIntakes(userId)
            Log.d("FoodRepository", "Cleared food data for user $userId")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error clearing user food data", e)
        }
    }

    // Helper method untuk check user exists
    suspend fun checkUserExists(userId: Int): Boolean {
        return try {
            val user = userDao.getUserById(userId)
            val exists = user != null
            Log.d("FoodRepository", "User $userId exists: $exists")
            exists
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error checking if user exists", e)
            false
        }
    }

    // API operations (untuk search dan sync nanti)
    suspend fun searchFood(query: String): List<FoodItem>? {
        return try {
            val response = apiService.searchFood(query)
            Log.d("FoodRepository", "API response: $response")
            response.foods?.takeIf { it.isNotEmpty() } ?: emptyList()
        } catch (e: Exception) {
            Log.e("FoodRepository", "Search food error: ${e.message}", e)
            null
        }
    }

    suspend fun getFoodById(foodId: String): FoodItem {
        val response = apiService.searchFood(foodId)
        return response.foods.firstOrNull()
            ?: throw IllegalStateException("Food with ID $foodId not found")
    }

    // Sync operations - TIDAK menghapus data lokal
    suspend fun syncToPostgres(userId: Int, token: String, intakes: List<FoodIntake>) {
        try {
            Log.d("FoodRepository", "Syncing ${intakes.size} intakes to server")
            intakes.forEach { intake ->
                val request = FoodIntakeRequest(
                    user_id = intake.userId,
                    food_name = intake.foodName,
                    calories = intake.calories.toDouble(),
                    protein = intake.protein.toDouble(),
                    fat = intake.fat.toDouble(),
                    carbs = intake.carbs.toDouble(),
                    amount_grams = intake.amountGrams.toDouble()
                )
                try {
                    val response = apiService.saveFoodIntake("Bearer $token", request)
                    if (response.isSuccessful) {
                        Log.d("FoodRepository", "Sync successful for: ${intake.foodName}")
                    } else {
                        Log.w("FoodRepository", "Sync failed for ${intake.foodName}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("FoodRepository", "Sync error for ${intake.foodName}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "General sync error", e)
        }
    }

    suspend fun getFoodIntakeHistory(userId: Int, token: String): List<FoodIntake> {
        return try {
            val response = apiService.getFoodIntakeHistory("Bearer $token", userId)
            val serverIntakes = response.map { intake ->
                FoodIntake(
                    id = intake.id,
                    userId = intake.userId,
                    foodName = intake.foodName,
                    calories = intake.calories.toFloat(),
                    protein = intake.protein.toFloat(),
                    fat = intake.fat.toFloat(),
                    carbs = intake.carbs.toFloat(),
                    amountGrams = intake.amountGrams.toFloat(),
                    timestamp = intake.timestamp.toLongOrNull() ?: System.currentTimeMillis()
                )
            }
            Log.d("FoodRepository", "Fetched ${serverIntakes.size} intakes from server")
            serverIntakes
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error fetching history from server: ${e.message}", e)
            emptyList()
        }
    }

    // DEPRECATED: Method yang berbahaya karena menghapus data lokal
    @Deprecated("Use safeSyncToServer instead to avoid data loss")
    suspend fun resetAndSyncDailyIntakes(userId: Int, token: String, date: LocalDate) {
        Log.w("FoodRepository", "resetAndSyncDailyIntakes is deprecated and should not be used")
        // Method ini tidak akan menghapus data lokal lagi
        safeSyncToServer(userId, token, date)
    }

    // Method yang aman untuk sync tanpa menghapus data lokal
    suspend fun safeSyncToServer(userId: Int, token: String, date: LocalDate) {
        try {
            val localIntakes = getDailyFoodIntakes(userId, date)
            if (localIntakes.isNotEmpty()) {
                syncToPostgres(userId, token, localIntakes)
                Log.d("FoodRepository", "Safe sync completed for ${localIntakes.size} intakes")
            } else {
                Log.d("FoodRepository", "No local intakes to sync for date $date")
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "Safe sync failed", e)
        }
    }

    // Method untuk sync data dari server ke lokal (tanpa menghapus yang sudah ada)
    suspend fun syncFromServerToLocal(userId: Int, token: String, date: LocalDate) {
        try {
            // Pastikan user exists dulu
            val userExists = checkUserExists(userId)
            if (!userExists) {
                Log.w("FoodRepository", "User $userId does not exist, cannot sync from server")
                return
            }

            val serverIntakes = getFoodIntakeHistory(userId, token)
            val startTime = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val endTime = date.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

            // Filter untuk hari yang diminta
            val dailyServerIntakes = serverIntakes.filter { intake ->
                intake.timestamp in startTime..endTime
            }

            // Insert yang belum ada (berdasarkan timestamp dan foodName)
            val localIntakes = getDailyFoodIntakes(userId, date)
            val newIntakes = dailyServerIntakes.filter { serverIntake ->
                localIntakes.none { localIntake ->
                    localIntake.foodName == serverIntake.foodName &&
                            Math.abs(localIntake.timestamp - serverIntake.timestamp) < 60000 // 1 minute tolerance
                }
            }

            newIntakes.forEach { intake ->
                insertFoodIntake(intake.copy(id = 0)) // Reset ID for new insert
            }

            Log.d("FoodRepository", "Synced ${newIntakes.size} new intakes from server")
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error syncing from server to local", e)
        }
    }
}