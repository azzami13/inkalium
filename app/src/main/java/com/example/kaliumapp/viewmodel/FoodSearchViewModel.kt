package com.example.kaliumapp.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.database.entities.FoodIntake
import com.example.kaliumapp.data.repository.FoodRepository
import com.example.kaliumapp.model.FoodItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {
    private val _searchResults = mutableStateOf<List<FoodItem>>(emptyList())
    val searchResults: State<List<FoodItem>> = _searchResults

    private val _selectedFoods = mutableStateOf<List<FoodItem>>(emptyList())
    val selectedFoods: State<List<FoodItem>> = _selectedFoods

    private val _selectedFood = MutableStateFlow<FoodItem?>(null)
    val selectedFood: StateFlow<FoodItem?> = _selectedFood

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun searchFood(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val results = repository.searchFood(query) ?: emptyList()
                Log.d("FoodSearchViewModel", "Search results: $results")
                _searchResults.value = results
                if (results.isEmpty()) {
                    _errorMessage.value = "No foods found for query: $query"
                }
            } catch (e: Exception) {
                Log.e("FoodSearchViewModel", "Search error: ${e.message}", e)
                _errorMessage.value = "Failed to search food: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getFoodDetails(foodId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val foodItem = repository.getFoodById(foodId)
                Log.d("FoodSearchViewModel", "Food details: $foodItem")
                _selectedFood.value = foodItem
            } catch (e: Exception) {
                Log.e("FoodSearchViewModel", "Get food details error: ${e.message}", e)
                _errorMessage.value = "Failed to fetch food details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFoodSelection(food: FoodItem) {
        val updatedList = _selectedFoods.value.toMutableList()
        if (food.isSelected) {
            updatedList.remove(food.copy(isSelected = false))
        } else {
            updatedList.add(food.copy(isSelected = true))
        }
        _selectedFoods.value = updatedList
    }

    fun logSelectedFoods(userId: Int, token: String) {
        viewModelScope.launch {
            try {
                _selectedFoods.value.forEach { food ->
                    val grams = food.amountGrams
                    repository.insertFoodIntake(
                        FoodIntake(
                            userId = userId,
                            foodName = food.description,
                            calories = calculateNutrient(food.nutrients["Energy"], grams),
                            protein = calculateNutrient(food.nutrients["Protein"], grams),
                            fat = calculateNutrient(food.nutrients["Total lipid (fat)"], grams),
                            carbs = calculateNutrient(food.nutrients["Carbohydrate, by difference"], grams),
                            amountGrams = grams,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _selectedFoods.value = emptyList()
            } catch (e: Exception) {
                Log.e("FoodSearchViewModel", "Log food error: ${e.message}", e)
                _errorMessage.value = "Failed to log food: ${e.message}"
            }
        }
    }

    fun logFood(userId: Int, food: FoodItem) {
        viewModelScope.launch {
            try {
                val grams = food.amountGrams
                repository.insertFoodIntake(
                    FoodIntake(
                        userId = userId,
                        foodName = food.description,
                        calories = calculateNutrient(food.nutrients["Energy"], grams),
                        protein = calculateNutrient(food.nutrients["Protein"], grams),
                        fat = calculateNutrient(food.nutrients["Total lipid (fat)"], grams),
                        carbs = calculateNutrient(food.nutrients["Carbohydrate, by difference"], grams),
                        amountGrams = grams,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e("FoodSearchViewModel", "Log food error: ${e.message}", e)
                _errorMessage.value = "Failed to log food: ${e.message}"
            }
        }
    }

    private fun calculateNutrient(nutrient: Float?, grams: Float): Float {
        return nutrient?.let { (it * grams) / 100f } ?: 0f
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}