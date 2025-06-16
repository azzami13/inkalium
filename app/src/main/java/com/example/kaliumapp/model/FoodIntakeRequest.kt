package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class FoodIntakeRequest(
    @SerializedName("user_id") val user_id: Int,
    @SerializedName("food_name") val food_name: String,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("amount_grams") val amount_grams: Double
)