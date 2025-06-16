package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class FoodItem(
    @SerializedName("description") val description: String,
    @SerializedName("nutrients") val nutrients: Map<String, Float>,
    @SerializedName("amountGrams") val amountGrams: Float = 100f,
    @SerializedName("isSelected") val isSelected: Boolean = false,
    @SerializedName("fdcId") val fdcId: Int? = null // Tambahkan fdcId untuk kompatibilitas
)