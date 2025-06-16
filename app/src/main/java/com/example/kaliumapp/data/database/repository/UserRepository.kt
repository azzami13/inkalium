package com.example.kaliumapp.data.repository

import android.content.Context
import android.util.Log
import com.example.kaliumapp.data.database.dao.UserDao
import com.example.kaliumapp.data.database.entities.User
import com.example.kaliumapp.model.UserResponse
import com.example.kaliumapp.remote.ApiService
import com.example.kaliumapp.remote.SharedPreferencesHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val context: Context
) {

    // ===== AUTHENTICATION & VALIDATION =====

    suspend fun validateToken(token: String): Boolean {
        return try {
            val response = apiService.getCurrentUser("Bearer $token")
            val isValid = response.isSuccessful && response.body() != null
            Log.d("UserRepository", "Token validation result: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e("UserRepository", "Error validating token", e)
            false
        }
    }

    suspend fun getCurrentUserId(token: String): Int? {
        return try {
            // First try to get from SharedPreferences (faster)
            val userId = SharedPreferencesHelper.getUserId(context)
            if (userId > 0) {
                Log.d("UserRepository", "UserId from SharedPreferences: $userId")
                return userId
            }

            // Fallback: get from API
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                SharedPreferencesHelper.saveUserBasicInfo(context, user.id, user.email ?: "", user.username)
                Log.d("UserRepository", "UserId from API: ${user.id}")
                user.id
            } else {
                Log.e("UserRepository", "Failed to get userId from API: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user ID", e)
            null
        }
    }

    // ===== USER CRUD OPERATIONS =====

    suspend fun insertUser(user: User) {
        try {
            Log.d("UserRepository", "Attempting to insert user: ${user.email}, ID: ${user.id}")

            // Check if user already exists
            val existingUser = getUserById(user.id)
            if (existingUser != null) {
                Log.d("UserRepository", "User already exists, updating instead")
                updateUser(user)
                return
            }

            // Insert new user
            userDao.insertUser(user)
            Log.d("UserRepository", "User inserted successfully: ${user.email}")

            // Verify insertion
            val verifyUser = getUserById(user.id)
            if (verifyUser != null) {
                Log.d("UserRepository", "User insertion verified: ${verifyUser.email}")
            } else {
                Log.e("UserRepository", "User insertion verification failed")
            }

        } catch (e: Exception) {
            Log.e("UserRepository", "Error inserting user: ${user.email}", e)
            throw e
        }
    }

    suspend fun getUserById(userId: Int): User? {
        return try {
            val user = userDao.getUserById(userId)
            Log.d("UserRepository", "getUserById($userId) result: ${user?.email ?: "null"}")
            user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by ID: $userId", e)
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            val user = userDao.getUserByEmail(email)
            Log.d("UserRepository", "getUserByEmail($email) result: ${user?.email ?: "null"}")
            user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by email: $email", e)
            null
        }
    }

    suspend fun updateUser(user: User) {
        try {
            userDao.updateUser(user)
            Log.d("UserRepository", "User updated successfully: ${user.email}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user: ${user.email}", e)
            throw e
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val user = userDao.getCurrentUser()
            Log.d("UserRepository", "getCurrentUser result: ${user?.email ?: "null"}")
            user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting current user", e)
            null
        }
    }

    suspend fun updateLoginStats(userId: Int, lastLogin: Long) {
        try {
            userDao.updateLoginStats(userId, lastLogin)
            Log.d("UserRepository", "Login stats updated for user: $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating login stats for user: $userId", e)
        }
    }

    // ===== API OPERATIONS =====

    suspend fun fetchUserProfileFromApi(token: String): UserResponse? {
        return try {
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val userResponse = response.body()!!

                // Save to Room database
                val localUser = User(
                    id = userResponse.id,
                    email = userResponse.email ?: "",
                    hashedPassword = "",
                    username = userResponse.username ?: "User",
                    role = "user",
                    umur = userResponse.umur ?: 0,
                    jenisKelamin = userResponse.jenisKelamin ?: "",
                    beratBadan = userResponse.beratBadan ?: 0.0,
                    tinggiBadan = userResponse.tinggiBadan ?: 0.0,
                    profileImage = userResponse.profileImage,
                    lastLogin = System.currentTimeMillis(),
                    isActive = true
                )

                insertUser(localUser)
                Log.d("UserRepository", "User profile fetched from API and saved to Room")
                userResponse
            } else {
                Log.e("UserRepository", "Failed to fetch profile from API: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user profile from API", e)
            null
        }
    }

    suspend fun updateUserProfile(user: User) {
        try {
            updateUser(user)
            Log.d("UserRepository", "User profile updated in Room: ${user.email}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user profile", e)
            throw e
        }
    }

    suspend fun updateUserToApi(userResponse: UserResponse) {
        try {
            val token = SharedPreferencesHelper.getToken(context)
            if (token != null) {
                val response = apiService.updateUserProfile("Bearer $token", userResponse)
                if (response.isSuccessful) {
                    Log.d("UserRepository", "User profile updated to API successfully")
                } else {
                    Log.e("UserRepository", "Failed to update profile to API: ${response.code()}")
                }
            } else {
                Log.e("UserRepository", "No token available for API update")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user to API", e)
            throw e
        }
    }

    suspend fun uploadImage(file: File): String? {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.uploadProfilePhoto(body)
            if (response.isSuccessful) {
                val filename = response.body()?.string() ?: file.name
                Log.d("UserRepository", "Image uploaded successfully: $filename")
                filename
            } else {
                Log.e("UserRepository", "Failed to upload image: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error uploading image", e)
            null
        }
    }

    // ===== DATA MANAGEMENT =====

    suspend fun clearAllUserData() {
        try {
            Log.d("UserRepository", "Starting to clear all user data...")

            // Clear users table (this will cascade to related tables due to foreign keys)
            userDao.clearAllUsers()
            Log.d("UserRepository", "All user data cleared successfully")

        } catch (e: Exception) {
            Log.e("UserRepository", "Error clearing all user data", e)
            throw e
        }
    }

    suspend fun deleteUser(userId: Int) {
        try {
            userDao.deleteUser(userId)
            Log.d("UserRepository", "User deleted: $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user: $userId", e)
            throw e
        }
    }

    // ===== UTILITY FUNCTIONS =====

    suspend fun userExists(userId: Int): Boolean {
        return try {
            val exists = getUserById(userId) != null
            Log.d("UserRepository", "User $userId exists: $exists")
            exists
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking if user exists: $userId", e)
            false
        }
    }

    // Debug function
    suspend fun debugAllUsers() {
        try {
            Log.d("UserRepository", "=== DEBUG ALL USERS ===")
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                Log.d("UserRepository", "Current user: ID=${currentUser.id}, Email=${currentUser.email}")
            } else {
                Log.d("UserRepository", "No current user found")
            }

            val userCount = userDao.getUserCount()
            Log.d("UserRepository", "Total users in database: $userCount")
            Log.d("UserRepository", "=== END DEBUG ===")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error debugging users", e)
        }
    }
}