package com.example.kaliumapp.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.repository.CalorieExpenditureRepository
import com.example.kaliumapp.data.repository.UserRepository
import com.example.kaliumapp.model.CalorieExpenditureResponse
import com.example.kaliumapp.remote.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

@HiltViewModel
class CalorieExpenditureViewModel @Inject constructor(
    private val repository: CalorieExpenditureRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _caloriesBurned = MutableStateFlow(0f)
    val caloriesBurned: StateFlow<Float> = _caloriesBurned

    private val _calorieHistory = MutableStateFlow<List<CalorieExpenditureResponse>>(emptyList())
    val calorieHistory: StateFlow<List<CalorieExpenditureResponse>> = _calorieHistory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val steps = intent?.getFloatExtra("steps", 0f) ?: 0f
            viewModelScope.launch {
                val token = context?.let { SharedPreferencesHelper.getToken(it) } ?: run {
                    Log.e("CalorieExpenditureViewModel", "Token not found when receiving steps")
                    _errorMessage.value = "Token not found. Please login again."
                    return@launch
                }

                val userId = userRepository.getCurrentUserId(token) ?: run {
                    Log.e("CalorieExpenditureViewModel", "Failed to get userId when receiving steps")
                    _errorMessage.value = "Failed to get user ID. Please login again."
                    return@launch
                }

                Log.d("CalorieExpenditureViewModel", "Steps received: $steps for userId=$userId")
                val calories = repository.calculateCaloriesBurned(steps, userId)
                _caloriesBurned.value = calories
            }
        }
    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter("STEP_COUNT_UPDATE")
        ContextCompat.registerReceiver(
            context,
            stepReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterReceiver(context: Context) {
        try {
            context.unregisterReceiver(stepReceiver)
        } catch (e: Exception) {
            Log.w("CalorieExpenditureViewModel", "Receiver was not registered", e)
        }
    }

    fun fetchCalorieHistory(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SharedPreferencesHelper.getToken(context)
                if (token == null) {
                    Log.e("CalorieExpenditureViewModel", "Token not found when fetching history")
                    _errorMessage.value = "Token not found. Please login again."
                    return@launch
                }

                val userId = userRepository.getCurrentUserId(token)
                if (userId == null) {
                    Log.e("CalorieExpenditureViewModel", "Failed to get userId when fetching history")
                    _errorMessage.value = "Failed to get user ID. Please login again."
                    return@launch
                }

                // Load from Room database first
                val localHistory = repository.getCalorieExpenditureHistory(userId)
                Log.d("CalorieExpenditureViewModel", "Local history loaded: ${localHistory.size} entries")

                // Convert to CalorieExpenditureResponse format for UI
                val responseHistory = localHistory.map { expenditure ->
                    CalorieExpenditureResponse(
                        id = expenditure.id,
                        userId = expenditure.userId,
                        calories = expenditure.calories,
                        date = expenditure.date
                    )
                }

                _calorieHistory.value = responseHistory
                Log.d("CalorieExpenditureViewModel", "History successfully loaded: ${responseHistory.size} entries")
            } catch (e: Exception) {
                Log.e("CalorieExpenditureViewModel", "Error fetching history: ${e.message}")
                _errorMessage.value = "Failed to load history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveExpenditure(context: Context) {
        viewModelScope.launch {
            try {
                val token = SharedPreferencesHelper.getToken(context)
                if (token == null) {
                    Log.e("CalorieExpenditureViewModel", "Token not found when saving data")
                    _errorMessage.value = "Token not found. Please login again."
                    return@launch
                }

                val userId = userRepository.getCurrentUserId(token)
                if (userId == null) {
                    Log.e("CalorieExpenditureViewModel", "Failed to get userId when saving data")
                    _errorMessage.value = "Failed to get user ID. Please login again."
                    return@launch
                }

                Log.d("CalorieExpenditureViewModel", "Saving data for userId=$userId, calories=${_caloriesBurned.value}")

                // Save to API (background sync)
                try {
                    repository.saveCalorieExpenditure(
                        userId = userId,
                        calories = _caloriesBurned.value,
                        date = LocalDate.now().toString(),
                        token = token
                    )
                } catch (apiError: Exception) {
                    Log.w("CalorieExpenditureViewModel", "API sync failed but local data saved", apiError)
                }

                Log.d("CalorieExpenditureViewModel", "Data saved successfully, reloading history")
                fetchCalorieHistory(context)
            } catch (e: Exception) {
                Log.e("CalorieExpenditureViewModel", "Error saving data: ${e.message}")
                _errorMessage.value = "Failed to save data: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}