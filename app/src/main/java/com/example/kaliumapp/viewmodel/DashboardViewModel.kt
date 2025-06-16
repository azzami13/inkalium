package com.example.kaliumapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _days = MutableStateFlow(5)
    val days: StateFlow<Int> = _days

    private val _waterIntake = MutableStateFlow(2)
    val waterIntake: StateFlow<Int> = _waterIntake

    private val _foodCalories = MutableStateFlow(2000)
    val foodCalories: StateFlow<Int> = _foodCalories

    fun resetData() {
        viewModelScope.launch {
            _days.emit(0)
            _waterIntake.emit(0)
            _foodCalories.emit(0)
        }
    }

    fun updateDashboardData(newDays: Int, newWaterIntake: Int, newFoodCalories: Int) {
        viewModelScope.launch {
            _days.emit(newDays)
            _waterIntake.emit(newWaterIntake)
            _foodCalories.emit(newFoodCalories)
        }
    }
}
