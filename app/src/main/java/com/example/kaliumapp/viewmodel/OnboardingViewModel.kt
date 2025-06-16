package com.example.kaliumapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.database.entities.User
import com.example.kaliumapp.data.repository.UserRepository
import com.example.kaliumapp.model.UserResponse
import com.example.kaliumapp.remote.ApiService
import com.example.kaliumapp.remote.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _umur = MutableStateFlow("")
    val umur: StateFlow<String> = _umur

    private val _jenisKelamin = MutableStateFlow("")
    val jenisKelamin: StateFlow<String> = _jenisKelamin

    private val _beratBadan = MutableStateFlow("")
    val beratBadan: StateFlow<String> = _beratBadan

    private val _tinggiBadan = MutableStateFlow("")
    val tinggiBadan: StateFlow<String> = _tinggiBadan

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun updateUsername(value: String) {
        _username.value = value
    }

    fun updateUmur(value: String) {
        _umur.value = value
    }

    fun updateJenisKelamin(value: String) {
        _jenisKelamin.value = value
    }

    fun updateBeratBadan(value: String) {
        _beratBadan.value = value
    }

    fun updateTinggiBadan(value: String) {
        _tinggiBadan.value = value
    }

    suspend fun saveOnboardingData() {
        _saveState.value = SaveState.Saving

        try {
            val email = SharedPreferencesHelper.getEmail(context) ?: ""
            val userId = SharedPreferencesHelper.getUserId(context)

            // Prioritas 1: Simpan ke Room database
            val user = User(
                id = if (userId > 0) userId else 0,
                email = email,
                hashedPassword = "",
                username = _username.value,
                role = "user",
                umur = _umur.value.toIntOrNull() ?: 0,
                jenisKelamin = _jenisKelamin.value,
                beratBadan = _beratBadan.value.toDoubleOrNull() ?: 0.0,
                tinggiBadan = _tinggiBadan.value.toDoubleOrNull() ?: 0.0,
                isActive = true
            )

            userRepository.insertUser(user)
            Log.d("OnboardingViewModel", "Onboarding data saved to Room successfully")

            // Prioritas 2: Coba sync ke API di background
            viewModelScope.launch {
                try {
                    val token = SharedPreferencesHelper.getToken(context)
                    if (token != null) {
                        val userResponse = UserResponse(
                            id = user.id,
                            email = user.email,
                            username = user.username,
                            umur = user.umur,
                            jenisKelamin = user.jenisKelamin,
                            beratBadan = user.beratBadan,
                            tinggiBadan = user.tinggiBadan,
                            profileImage = user.profileImage
                        )

                        val response = apiService.updateUserProfile("Bearer $token", userResponse)
                        if (response.isSuccessful) {
                            Log.d("OnboardingViewModel", "Onboarding data synced to API successfully")
                            response.body()?.let { updatedUser ->
                                // Update Room dengan response dari API
                                val updatedLocalUser = user.copy(id = updatedUser.id)
                                userRepository.insertUser(updatedLocalUser)

                                // Update SharedPreferences dengan ID yang benar
                                SharedPreferencesHelper.saveUserBasicInfo(
                                    context,
                                    id = updatedUser.id,
                                    email = updatedUser.email ?: email,
                                    username = updatedUser.username
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OnboardingViewModel", "API sync failed but local save succeeded", e)
                }
            }

            // Tandai onboarding sudah selesai
            SharedPreferencesHelper.setOnboardingCompleted(context, true)
            _saveState.value = SaveState.Success

        } catch (e: Exception) {
            Log.e("OnboardingViewModel", "Error saving onboarding data", e)
            _saveState.value = SaveState.Error("Gagal menyimpan data: ${e.message}")
        }
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}
