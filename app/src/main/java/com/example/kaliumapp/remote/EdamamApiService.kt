package com.example.kaliumapp.remote

import com.example.kaliumapp.model.FoodSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EdamamApiService {
    @GET("api/food-database/v2/parser")
    suspend fun searchFood(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("ingr") query: String,
        @Query("nutrition-type") nutritionType: String = "cooking"
    ): FoodSearchResponse
}