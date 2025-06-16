package com.example.kaliumapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.repository.WaterRepository
import com.example.kaliumapp.data.database.entities.DailyWaterSummary
import com.example.kaliumapp.data.database.entities.WaterIntake
import com.example.kaliumapp.data.database.entities.WaterReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class WaterIntakeViewModel @Inject constructor(
    private val waterRepository: WaterRepository
) : ViewModel() {

    private val _waterIntake = MutableStateFlow(0)
    val waterIntake: StateFlow<Int> = _waterIntake

    private val _totalWaterTarget = MutableStateFlow(0)
    val totalWaterTarget: StateFlow<Int> = _totalWaterTarget

    private val _reminderTimes = MutableStateFlow<List<String>>(listOf())
    val reminderTimes: StateFlow<List<String>> = _reminderTimes

    private val _wakeUpTime = MutableStateFlow("06:00")
    val wakeUpTime: StateFlow<String> = _wakeUpTime

    private val _sleepTime = MutableStateFlow("22:00")
    val sleepTime: StateFlow<String> = _sleepTime

    private val _currentSummaryId = MutableStateFlow<Int?>(null)
    val currentSummaryId: StateFlow<Int?> = _currentSummaryId

    fun calculateWaterIntake(userId: Int, gender: String, weight: Double, age: Int) {
        viewModelScope.launch {
            try {
                Log.d("WaterIntakeViewModel", "Calculating water intake for user $userId")

                // Pastikan user ada di database dulu
                val user = waterRepository.getUserById(userId)
                if (user == null) {
                    Log.e("WaterIntakeViewModel", "User $userId not found in database")
                    return@launch
                }

                var requiredWater = (weight * 30).toInt()

                if (gender.equals("female", ignoreCase = true)) {
                    requiredWater = (requiredWater * 0.9).toInt()
                }

                if (age > 50) {
                    requiredWater = (requiredWater * 0.85).toInt()
                }

                _totalWaterTarget.value = requiredWater
                _waterIntake.value = 0

                // Buat atau ambil daily summary untuk hari ini
                createDailySummary(userId, requiredWater)

            } catch (e: Exception) {
                Log.e("WaterIntakeViewModel", "Error calculating water intake", e)
            }
        }
    }

    private suspend fun createDailySummary(userId: Int, targetWater: Int) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Cek apakah sudah ada summary untuk hari ini
            val existingSummary = waterRepository.getWaterSummary(userId, today)

            if (existingSummary != null) {
                _currentSummaryId.value = existingSummary.summaryId
                _waterIntake.value = existingSummary.totalIntakeMl
                Log.d("WaterIntakeViewModel", "Using existing water summary: ${existingSummary.summaryId}")
            } else {
                // Buat summary baru dengan ID yang valid
                val newSummary = DailyWaterSummary(
                    summaryId = 0, // Auto-generate
                    userId = userId,
                    date = today,
                    totalIntakeMl = 0,
                    remainingMl = targetWater,
                    status = "Active"
                )

                val summaryId = waterRepository.insertWaterSummary(newSummary)
                _currentSummaryId.value = summaryId.toInt()
                Log.d("WaterIntakeViewModel", "Created new water summary: $summaryId")
            }
        } catch (e: Exception) {
            Log.e("WaterIntakeViewModel", "Error creating daily summary", e)
        }
    }

    fun setWakeAndSleepTimes(wakeUp: String, sleep: String) {
        _wakeUpTime.value = wakeUp
        _sleepTime.value = sleep
    }

    fun setReminderTimes(numberOfReminders: Int, reminderCount: Int) {
        val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(_wakeUpTime.value)
        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(_sleepTime.value)

        val reminderList = mutableListOf<String>()

        if (startTime != null && endTime != null) {
            val timeDiff = endTime.time - startTime.time
            val interval = timeDiff / (numberOfReminders + 1)

            for (i in 1..numberOfReminders) {
                val reminderTimeMillis = startTime.time + (i * interval)
                val reminderTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminderTimeMillis))
                reminderList.add(reminderTime)
            }
        }

        _reminderTimes.value = reminderList

        // Simpan reminders ke Room database
        saveRemindersToRoom(reminderList)
    }

    private fun saveRemindersToRoom(reminderTimes: List<String>) {
        viewModelScope.launch {
            try {
                _currentSummaryId.value?.let { summaryId ->
                    reminderTimes.forEach { timeString ->
                        try {
                            val reminderTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeString)
                            reminderTime?.let {
                                val reminder = WaterReminder(
                                    reminderId = 0, // Auto-generate
                                    summaryId = summaryId,
                                    reminderTime = it.time,
                                    status = "Pending"
                                )
                                waterRepository.insertWaterReminder(reminder)
                            }
                        } catch (e: Exception) {
                            Log.e("WaterIntakeViewModel", "Error saving reminder", e)
                        }
                    }
                    Log.d("WaterIntakeViewModel", "Reminders saved to Room database")
                }
            } catch (e: Exception) {
                Log.e("WaterIntakeViewModel", "Error saving reminders to Room", e)
            }
        }
    }

    fun addWaterIntake(amount: Int) {
        viewModelScope.launch {
            try {
                _currentSummaryId.value?.let { summaryId ->
                    // Pastikan summary masih ada
                    val summary = waterRepository.getWaterSummaryById(summaryId)
                    if (summary == null) {
                        Log.e("WaterIntakeViewModel", "Summary $summaryId not found")
                        return@launch
                    }

                    // Simpan intake ke Room database
                    val intake = WaterIntake(
                        intakeId = 0, // Auto-generate
                        summaryId = summaryId,
                        intakeMl = amount,
                        intakeTime = System.currentTimeMillis()
                    )
                    waterRepository.insertWaterIntake(intake)

                    // Update local state
                    val newIntake = minOf(_waterIntake.value + amount, _totalWaterTarget.value)
                    _waterIntake.value = newIntake

                    // Update summary di database
                    val updatedSummary = summary.copy(
                        totalIntakeMl = newIntake,
                        remainingMl = _totalWaterTarget.value - newIntake
                    )
                    waterRepository.updateWaterSummary(updatedSummary)

                    Log.d("WaterIntakeViewModel", "Water intake added: ${amount}ml, total: ${newIntake}ml")
                } ?: run {
                    Log.e("WaterIntakeViewModel", "No current summary ID available")
                }
            } catch (e: Exception) {
                Log.e("WaterIntakeViewModel", "Error adding water intake", e)
            }
        }
    }

    fun loadTodayWaterData(userId: Int) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                Log.d("WaterIntakeViewModel", "Loading water data for user $userId, date $today")

                // Pastikan user exists
                val user = waterRepository.getUserById(userId)
                if (user == null) {
                    Log.e("WaterIntakeViewModel", "User $userId not found")
                    return@launch
                }

                val summary = waterRepository.getWaterSummary(userId, today)

                summary?.let {
                    _currentSummaryId.value = it.summaryId

                    // Get actual total from intake records
                    val actualTotal = waterRepository.getTotalWaterIntake(it.summaryId)
                    _waterIntake.value = actualTotal

                    // Calculate target properly
                    val targetWater = actualTotal + it.remainingMl
                    _totalWaterTarget.value = targetWater

                    // Load reminders
                    val reminders = waterRepository.getWaterReminders(it.summaryId)
                    val reminderTimeStrings = reminders.map { reminder ->
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reminder.reminderTime))
                    }
                    _reminderTimes.value = reminderTimeStrings

                    Log.d("WaterIntakeViewModel",
                        "Today's water data loaded - Summary ID: ${it.summaryId}, " +
                                "Actual Total: $actualTotal ml, Target: $targetWater ml, " +
                                "Reminders: ${reminders.size}")
                } ?: run {
                    Log.d("WaterIntakeViewModel", "No water summary found for today - resetting values")
                    _currentSummaryId.value = null
                    _waterIntake.value = 0
                    _totalWaterTarget.value = 0
                    _reminderTimes.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("WaterIntakeViewModel", "Error loading today's water data", e)
            }
        }
    }

    fun refreshWaterData(userId: Int) {
        Log.d("WaterIntakeViewModel", "Refreshing water data for user $userId")
        loadTodayWaterData(userId)
    }
}