package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class CalorieExpenditureRequest(
    @SerializedName("user_id") val userId: Int, // Mapped to "user_id" in JSON
    @SerializedName("calories") val calories: Float,
    @SerializedName("date") val date: String
)