package com.example.kaliumapp.model

data class FoodSearchRequest(
    val query: String,
    val pageSize: Int = 10
)