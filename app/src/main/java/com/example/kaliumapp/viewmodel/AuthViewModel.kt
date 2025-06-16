package com.example.kaliumapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.database.dao.UserDao
import com.example.kaliumapp.data.database.entities.User
import com.example.kaliumapp.model.AuthRequest
import com.example.kaliumapp.model.AuthResponse
import com.example.kaliumapp.remote.ApiService
import com.example.kaliumapp.remote.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao, // Direct DAO injection
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun checkLoginStatus() {
        if (SharedPreferencesHelper.isLoggedIn(context)) {
            _authState.value = AuthState.AlreadyLoggedIn
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email dan password harus diisi")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("AuthViewModel", "Starting login process for: $email")

                val response = apiService.login(AuthRequest(email, password))
                if (response.success && response.token != null) {
                    Log.d("AuthViewModel", "✅ Login API successful, token received")

                    // Save token immediately
                    SharedPreferencesHelper.saveToken(context, response.token)

                    // Get user data from API
                    try {
                        val userResponse = apiService.getCurrentUser("Bearer ${response.token}")
                        if (userResponse.isSuccessful && userResponse.body() != null) {
                            val apiUser = userResponse.body()!!
                            Log.d("AuthViewModel", "✅ User data received: ID=${apiUser.id}, Email=${apiUser.email}")

                            // Save to SharedPreferences
                            SharedPreferencesHelper.saveUserBasicInfo(
                                context,
                                id = apiUser.id,
                                email = apiUser.email ?: email,
                                username = apiUser.username
                            )

                            // FORCE SAVE to database with multiple attempts
                            forceSaveUserToDatabase(apiUser, email)

                            _authState.value = AuthState.Success(response)
                        } else {
                            Log.e("AuthViewModel", "❌ Failed to get user data: ${userResponse.code()}")
                            handleLoginWithoutUserData(email, response)
                        }
                    } catch (apiException: Exception) {
                        Log.e("AuthViewModel", "❌ API error getting user data", apiException)
                        handleLoginWithoutUserData(email, response)
                    }
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Login gagal")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Login error", e)
                _authState.value = AuthState.Error("Terjadi kesalahan jaringan: ${e.message}")
            }
        }
    }

    private suspend fun forceSaveUserToDatabase(apiUser: com.example.kaliumapp.model.UserResponse, email: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AuthViewModel", "🔄 FORCE SAVING user to database...")

                // Create user entity
                val localUser = User(
                    id = apiUser.id,
                    email = apiUser.email ?: email,
                    hashedPassword = "",
                    username = apiUser.username ?: "User",
                    role = "user",
                    umur = apiUser.umur ?: 0,
                    jenisKelamin = apiUser.jenisKelamin ?: "",
                    beratBadan = apiUser.beratBadan ?: 0.0,
                    tinggiBadan = apiUser.tinggiBadan ?: 0.0,
                    profileImage = apiUser.profileImage,
                    lastLogin = System.currentTimeMillis(),
                    loginCount = 1,
                    isActive = true
                )

                Log.d("AuthViewModel", "📝 User entity created: $localUser")

                // Multiple save attempts with different strategies
                var saveSuccess = false

                // Attempt 1: Direct insert
                try {
                    Log.d("AuthViewModel", "🔄 Attempt 1: Direct insert")
                    userDao.insertUser(localUser)
                    saveSuccess = true
                    Log.d("AuthViewModel", "✅ Attempt 1: SUCCESS")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Attempt 1 failed", e)
                }

                // Attempt 2: Delete then insert (if first attempt failed)
                if (!saveSuccess) {
                    try {
                        Log.d("AuthViewModel", "🔄 Attempt 2: Delete then insert")
                        userDao.deleteUser(apiUser.id)
                        Thread.sleep(100) // Small delay
                        userDao.insertUser(localUser)
                        saveSuccess = true
                        Log.d("AuthViewModel", "✅ Attempt 2: SUCCESS")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Attempt 2 failed", e)
                    }
                }

                // Attempt 3: Clear all then insert (if second attempt failed)
                if (!saveSuccess) {
                    try {
                        Log.d("AuthViewModel", "🔄 Attempt 3: Clear all then insert")
                        userDao.clearAllUsers()
                        Thread.sleep(200) // Longer delay
                        userDao.insertUser(localUser)
                        saveSuccess = true
                        Log.d("AuthViewModel", "✅ Attempt 3: SUCCESS")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Attempt 3 failed", e)
                    }
                }

                // Verify save
                if (saveSuccess) {
                    try {
                        val savedUser = userDao.getUserById(apiUser.id)
                        if (savedUser != null) {
                            Log.d("AuthViewModel", "✅ VERIFICATION SUCCESS: User found in database")
                            Log.d("AuthViewModel", "📋 Saved user details: ID=${savedUser.id}, Email=${savedUser.email}")
                        } else {
                            Log.e("AuthViewModel", "❌ VERIFICATION FAILED: User not found after save")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Verification error", e)
                    }
                } else {
                    Log.e("AuthViewModel", "❌ ALL SAVE ATTEMPTS FAILED")

                    // Last resort: Create a test user to see if database works at all
                    try {
                        val testUser = User(
                            id = 999,
                            email = "test@test.com",
                            hashedPassword = "",
                            username = "Test",
                            role = "user",
                            umur = 25,
                            jenisKelamin = "Male",
                            beratBadan = 70.0,
                            tinggiBadan = 170.0,
                            isActive = true
                        )
                        userDao.insertUser(testUser)
                        Log.d("AuthViewModel", "✅ Test user saved successfully - database is working")

                        // Try original user again
                        userDao.insertUser(localUser)
                        Log.d("AuthViewModel", "✅ Original user saved after test")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Even test user failed - database issue", e)
                    }
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Critical error in forceSaveUserToDatabase", e)
            }
        }
    }

    private suspend fun handleLoginWithoutUserData(email: String, response: AuthResponse) {
        Log.d("AuthViewModel", "Handling login without complete user data")
        SharedPreferencesHelper.saveUserBasicInfo(
            context,
            id = -1,
            email = email,
            username = null
        )
        _authState.value = AuthState.Success(response)
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email dan password harus diisi")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Password dan konfirmasi password tidak cocok")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(AuthRequest(email, password))
                if (response.success && response.token != null) {
                    Log.d("AuthViewModel", "Register API successful")

                    SharedPreferencesHelper.saveToken(context, response.token)
                    SharedPreferencesHelper.setNewUser(context, true)

                    try {
                        val userResponse = apiService.getCurrentUser("Bearer ${response.token}")
                        if (userResponse.isSuccessful && userResponse.body() != null) {
                            val apiUser = userResponse.body()!!

                            SharedPreferencesHelper.saveUserBasicInfo(
                                context,
                                id = apiUser.id,
                                email = apiUser.email ?: email,
                                username = apiUser.username
                            )

                            forceSaveUserToDatabase(apiUser, email)
                            _authState.value = AuthState.Success(response)
                        } else {
                            handleLoginWithoutUserData(email, response)
                        }
                    } catch (apiException: Exception) {
                        Log.e("AuthViewModel", "API error during registration", apiException)
                        handleLoginWithoutUserData(email, response)
                    }
                } else {
                    _authState.value = AuthState.Error(response.message ?: "Registrasi gagal")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saat registrasi", e)
                _authState.value = AuthState.Error("Terjadi kesalahan jaringan: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting logout process...")

                userDao.clearAllUsers()
                SharedPreferencesHelper.clearAll(context)

                _authState.value = AuthState.Idle
                Log.d("AuthViewModel", "Logout completed")

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during logout", e)
                SharedPreferencesHelper.clearAll(context)
                _authState.value = AuthState.Idle
            }
        }
    }

    // Manual debug functions
    fun manualSaveUser() {
        viewModelScope.launch {
            try {
                val userId = SharedPreferencesHelper.getUserId(context)
                val email = SharedPreferencesHelper.getEmail(context)

                if (userId > 0 && email != null) {
                    val testUser = User(
                        id = userId,
                        email = email,
                        hashedPassword = "",
                        username = "Manual User",
                        role = "user",
                        umur = 25,
                        jenisKelamin = "Male",
                        beratBadan = 70.0,
                        tinggiBadan = 170.0,
                        isActive = true,
                        lastLogin = System.currentTimeMillis()
                    )

                    userDao.clearAllUsers()
                    userDao.insertUser(testUser)

                    val saved = userDao.getUserById(userId)
                    Log.d("AuthViewModel", "Manual save result: ${saved?.email}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Manual save failed", e)
            }
        }
    }

    fun debugDatabase() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "=== DATABASE DEBUG ===")

                val userCount = userDao.getUserCount()
                Log.d("AuthViewModel", "Total users in database: $userCount")

                val allUsers = userDao.getAllUsers()
                allUsers.forEach { user ->
                    Log.d("AuthViewModel", "User: ID=${user.id}, Email=${user.email}")
                }

                val currentUser = userDao.getCurrentUser()
                Log.d("AuthViewModel", "Current user: ${currentUser?.email}")

                val userId = SharedPreferencesHelper.getUserId(context)
                if (userId > 0) {
                    val userById = userDao.getUserById(userId)
                    Log.d("AuthViewModel", "User by ID $userId: ${userById?.email}")
                }

                Log.d("AuthViewModel", "=== END DEBUG ===")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Debug failed", e)
            }
        }
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object AlreadyLoggedIn : AuthState()
        data class Success(val response: AuthResponse) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}