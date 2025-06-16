package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class FoodIntakeResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("amount_grams") val amountGrams: Double,
    @SerializedName("timestamp") val timestamp: String // Menggunakan String untuk format ISO
)