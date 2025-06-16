package com.example.kaliumapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.repository.UserRepository
import com.example.kaliumapp.model.UserResponse
import com.example.kaliumapp.remote.SharedPreferencesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Initial)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // Prioritas 1: Load dari Room database
                val localUser = userRepository.getCurrentUser()
                if (localUser != null) {
                    val userResponse = UserResponse(
                        id = localUser.id,
                        email = localUser.email,
                        username = localUser.username,
                        umur = localUser.umur,
                        jenisKelamin = localUser.jenisKelamin,
                        beratBadan = localUser.beratBadan,
                        tinggiBadan = localUser.tinggiBadan,
                        profileImage = localUser.profileImage
                    )
                    _profileState.value = ProfileState.Success(userResponse)
                    Log.d("ProfileViewModel", "Profile loaded from Room database")
                } else {
                    // Fallback: Load dari API jika Room kosong
                    val token = SharedPreferencesHelper.getToken(context)
                    if (token != null) {
                        loadProfileFromApi(token)
                    } else {
                        _profileState.value = ProfileState.Error("Anda belum login")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
                _profileState.value = ProfileState.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private suspend fun loadProfileFromApi(token: String) {
        try {
            val user = userRepository.fetchUserProfileFromApi(token)
            if (user != null) {
                _profileState.value = ProfileState.Success(user)
                Log.d("ProfileViewModel", "Profile loaded from API and saved to Room")
            } else {
                _profileState.value = ProfileState.Error("Gagal mengambil data profil")
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error fetching profile from API", e)
            _profileState.value = ProfileState.Error("Terjadi kesalahan: ${e.message}")
        }
    }

    fun updateProfileToApi(updatedUser: UserResponse) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // Simpan ke Room database terlebih dahulu
                val localUser = userRepository.getUserById(updatedUser.id)
                localUser?.let { existingUser ->
                    val updatedLocalUser = existingUser.copy(
                        username = updatedUser.username ?: existingUser.username,
                        umur = updatedUser.umur ?: existingUser.umur,
                        jenisKelamin = updatedUser.jenisKelamin ?: existingUser.jenisKelamin,
                        beratBadan = updatedUser.beratBadan ?: existingUser.beratBadan,
                        tinggiBadan = updatedUser.tinggiBadan ?: existingUser.tinggiBadan,
                        profileImage = updatedUser.profileImage ?: existingUser.profileImage
                    )
                    userRepository.updateUserProfile(updatedLocalUser)
                }

                _profileState.value = ProfileState.Success(updatedUser)
                Log.d("ProfileViewModel", "Profile updated in Room database")

                // Coba sync ke API di background
                try {
                    userRepository.updateUserToApi(updatedUser)
                    Log.d("ProfileViewModel", "Profile synced to API successfully")
                } catch (apiError: Exception) {
                    Log.w("ProfileViewModel", "API sync failed but local update succeeded", apiError)
                    // Tidak mengubah state karena local update berhasil
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile", e)
                _profileState.value = ProfileState.Error("Gagal memperbarui profil: ${e.message}")
            }
        }
    }

    fun uploadImageToServer(imagePath: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(imagePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        _profileState.value = ProfileState.Error("File gambar tidak ditemukan")
                    }
                    return@launch
                }

                val response = userRepository.uploadImage(file)
                withContext(Dispatchers.Main) {
                    response?.let { filename ->
                        onSuccess(filename)
                        Log.d("ProfileViewModel", "Image uploaded successfully: $filename")
                    } ?: run {
                        _profileState.value = ProfileState.Error("Gagal mengunggah gambar")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProfileViewModel", "Error uploading image", e)
                    _profileState.value = ProfileState.Error("Gagal mengunggah gambar: ${e.message}")
                }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                // Clear all data from Room and SharedPreferences
                userRepository.clearAllUserData()
                SharedPreferencesHelper.clearAll(context)

                _profileState.value = ProfileState.Initial
                onLoggedOut()
                Log.d("ProfileViewModel", "Logout completed successfully")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during logout", e)
                // Tetap lanjutkan logout meskipun ada error
                try {
                    SharedPreferencesHelper.clearAll(context)
                } catch (clearError: Exception) {
                    Log.e("ProfileViewModel", "Error clearing SharedPreferences", clearError)
                }
                _profileState.value = ProfileState.Initial
                onLoggedOut()
            }
        }
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    fun clearError() {
        if (_profileState.value is ProfileState.Error) {
            _profileState.value = ProfileState.Initial
        }
    }

    sealed class ProfileState {
        object Initial : ProfileState()
        object Loading : ProfileState()
        data class Success(val user: UserResponse) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}