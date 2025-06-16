package com.example.kaliumapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.database.entities.FoodIntake
import com.example.kaliumapp.data.repository.FoodRepository
import com.example.kaliumapp.model.NutrientInfo
import com.example.kaliumapp.remote.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class CalorieIntakeViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {
    private val _dailyNutrients = MutableStateFlow<NutrientInfo?>(null)
    val dailyNutrients: StateFlow<NutrientInfo?> = _dailyNutrients.asStateFlow()

    private val _dailyIntakes = MutableStateFlow<List<FoodIntake>>(emptyList())
    val dailyIntakes: StateFlow<List<FoodIntake>> = _dailyIntakes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Method utama untuk refresh data (tanpa sync yang merusak)
    fun refreshDailyNutrients(userId: Int, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("CalorieIntakeViewModel", "Refreshing daily nutrients for user $userId, date $date")

                // Pastikan user exists dulu sebelum query
                val userExists = repository.checkUserExists(userId)
                if (!userExists) {
                    Log.e("CalorieIntakeViewModel", "User $userId does not exist in database")
                    _errorMessage.value = "User not found. Please login again."
                    return@launch
                }

                // Ambil data langsung dari Room database
                val foods = repository.getDailyFoodIntakes(userId, date)
                Log.d("CalorieIntakeViewModel", "Found ${foods.size} food intakes: $foods")

                _dailyIntakes.value = foods

                // Hitung total nutrisi
                val totalNutrients = calculateTotalNutrients(foods)
                _dailyNutrients.value = totalNutrients

                Log.d("CalorieIntakeViewModel", "Total nutrients: $totalNutrients")

            } catch (e: Exception) {
                Log.e("CalorieIntakeViewModel", "Failed to refresh daily nutrients", e)
                _errorMessage.value = "Failed to fetch daily nutrients: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Method untuk initial load dengan sync (opsional)
    fun updateDailyNutrients(userId: Int, date: LocalDate, context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("CalorieIntakeViewModel", "Initial load for user $userId, date $date")

                // Pastikan user exists dulu
                val userExists = repository.checkUserExists(userId)
                if (!userExists) {
                    Log.e("CalorieIntakeViewModel", "User $userId does not exist in database")
                    _errorMessage.value = "User not found. Please login again."
                    return@launch
                }

                val token = SharedPreferencesHelper.getToken(context)

                // Hanya sync jika ada token dan belum sync hari ini
                if (token != null) {
                    val lastSyncDate = SharedPreferencesHelper.getLastSyncDate(context) ?: ""
                    val currentDate = date.toString()

                    if (lastSyncDate != currentDate) {
                        Log.d("CalorieIntakeViewModel", "Syncing data for date $currentDate")
                        try {
                            // Sync ke server tapi JANGAN hapus data lokal
                            syncToServerOnly(userId, token, date)
                            SharedPreferencesHelper.saveLastSyncDate(context, currentDate)
                        } catch (syncError: Exception) {
                            Log.w("CalorieIntakeViewModel", "Sync failed but continuing with local data", syncError)
                        }
                    }
                }

                // Selalu load data lokal
                refreshDailyNutrients(userId, date)

            } catch (e: Exception) {
                Log.e("CalorieIntakeViewModel", "Failed to update daily nutrients", e)
                _errorMessage.value = "Failed to fetch daily nutrients: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Method untuk sync tanpa menghapus data lokal
    private suspend fun syncToServerOnly(userId: Int, token: String, date: LocalDate) {
        try {
            val localIntakes = repository.getDailyFoodIntakes(userId, date)
            if (localIntakes.isNotEmpty()) {
                repository.syncToPostgres(userId, token, localIntakes)
                Log.d("CalorieIntakeViewModel", "Synced ${localIntakes.size} intakes to server")
            }
        } catch (e: Exception) {
            Log.e("CalorieIntakeViewModel", "Sync to server failed", e)
            // Don't throw - continue with local data
        }
    }

    // Method untuk menghitung total nutrisi
    private fun calculateTotalNutrients(foods: List<FoodIntake>): NutrientInfo {
        return foods.fold(NutrientInfo(0f, 0f, 0f, 0f)) { acc, food ->
            NutrientInfo(
                calories = acc.calories + food.calories,
                protein = acc.protein + food.protein,
                fat = acc.fat + food.fat,
                carbs = acc.carbs + food.carbs
            )
        }
    }

    fun calculateNutrientPercentage(nutrient: String, value: Float): Float {
        val calorieGoal = 2000f
        val proteinMax = 50f
        val fatMax = 70f
        val carbsMax = 300f
        return when (nutrient) {
            "calories" -> (value / calorieGoal) * 100
            "protein" -> (value / proteinMax) * 100
            "fat" -> (value / fatMax) * 100
            "carbs" -> (value / carbsMax) * 100
            else -> 0f
        }.coerceIn(0f, 100f)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Method untuk menambah food intake (dipanggil dari FoodSearchScreen)
    fun addFoodIntake(userId: Int, foodIntake: FoodIntake) {
        viewModelScope.launch {
            try {
                Log.d("CalorieIntakeViewModel", "Adding food intake: $foodIntake")

                // Pastikan user exists dulu
                val userExists = repository.checkUserExists(userId)
                if (!userExists) {
                    Log.e("CalorieIntakeViewModel", "User $userId does not exist when adding food intake")
                    _errorMessage.value = "User not found. Please login again."
                    return@launch
                }

                // Pastikan foodIntake punya userId yang benar
                val correctedFoodIntake = foodIntake.copy(userId = userId)

                val insertedId = repository.insertFoodIntake(correctedFoodIntake)
                Log.d("CalorieIntakeViewModel", "Food intake inserted with ID: $insertedId")

                // Refresh data setelah menambah
                refreshDailyNutrients(userId)

            } catch (e: Exception) {
                Log.e("CalorieIntakeViewModel", "Failed to add food intake", e)
                _errorMessage.value = "Failed to add food intake: ${e.message}"
            }
        }
    }

    // Method untuk delete food intake
    fun deleteFoodIntake(userId: Int, foodIntakeId: Int) {
        viewModelScope.launch {
            try {
                Log.d("CalorieIntakeViewModel", "Deleting food intake: $foodIntakeId")

                repository.deleteFoodIntake(foodIntakeId)

                // Refresh data setelah menghapus
                refreshDailyNutrients(userId)

            } catch (e: Exception) {
                Log.e("CalorieIntakeViewModel", "Failed to delete food intake", e)
                _errorMessage.value = "Failed to delete food intake: ${e.message}"
            }
        }
    }

    // Method untuk update food intake
    fun updateFoodIntake(userId: Int, foodIntake: FoodIntake) {
        viewModelScope.launch {
            try {
                Log.d("CalorieIntakeViewModel", "Updating food intake: ${foodIntake.id}")

                // Pastikan foodIntake punya userId yang benar
                val correctedFoodIntake = foodIntake.copy(userId = userId)

                repository.updateFoodIntake(correctedFoodIntake)

                // Refresh data setelah update
                refreshDailyNutrients(userId)

            } catch (e: Exception) {
                Log.e("CalorieIntakeViewModel", "Failed to update food intake", e)
                _errorMessage.value = "Failed to update food intake: ${e.message}"
            }
        }
    }
}