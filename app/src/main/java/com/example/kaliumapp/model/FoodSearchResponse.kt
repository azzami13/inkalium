package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class FoodSearchResponse(
    @SerializedName("foodItems") val foods: List<FoodItem>,
    @SerializedName("totalResults") val totalResults: Int
)