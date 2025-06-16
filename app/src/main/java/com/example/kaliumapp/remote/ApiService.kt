package com.example.kaliumapp.remote

import com.example.kaliumapp.model.AuthRequest
import com.example.kaliumapp.model.AuthResponse
import com.example.kaliumapp.model.CalorieExpenditureRequest
import com.example.kaliumapp.model.CalorieExpenditureResponse
import com.example.kaliumapp.model.FoodIntakeRequest
import com.example.kaliumapp.model.FoodIntakeResponse
import com.example.kaliumapp.model.FoodItem
import com.example.kaliumapp.model.FoodSearchResponse
import com.example.kaliumapp.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @Headers("Content-Type: application/json")
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @Headers("Content-Type: application/json")
    @PUT("auth/me")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body user: UserResponse
    ): Response<UserResponse>

    @Multipart
    @POST("upload-photo")
    suspend fun uploadProfilePhoto(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("api/food/{id}")
    suspend fun getFoodById(@Path("id") id: String): FoodItem

    @POST("calorie-expenditure")
    suspend fun saveCalorieExpenditure(
        @Header("Authorization") token: String,
        @Body request: CalorieExpenditureRequest
    ): Response<CalorieExpenditureResponse>

    @GET("calorie-expenditure/history")
    suspend fun getCalorieExpenditureHistory(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int
    ): Response<List<CalorieExpenditureResponse>>

    @GET("api/food/search")
    suspend fun searchFood(@Query("query") query: String): FoodSearchResponse

    @POST("api/food/intake")
    suspend fun saveFoodIntake(
        @Header("Authorization") token: String,
        @Body request: FoodIntakeRequest
    ): Response<FoodIntakeResponse>

    @GET("api/food/intake/history")
    suspend fun getFoodIntakeHistory(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int
    ): List<FoodIntakeResponse>
}