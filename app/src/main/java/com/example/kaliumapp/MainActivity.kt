package com.example.kaliumapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.kaliumapp.data.repository.UserRepository
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.navigation.AppNavigation
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.ui.theme.KaliumTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            initializeApp()
        } else {
            Toast.makeText(this, "Permissions required for full functionality.", Toast.LENGTH_SHORT).show()
            // Initialize app anyway, some features might still work
            initializeApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        if (arePermissionsGranted()) {
            initializeApp()
        } else {
            requestPermissions()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val activityRecognitionPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationPermission && coarseLocationPermission && activityRecognitionPermission
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        )
    }

    private fun initializeApp() {
        CoroutineScope(Dispatchers.IO).launch {
            val token = SharedPreferencesHelper.getToken(this@MainActivity)
            val isValid = if (token != null) {
                userRepository.validateToken(token)
            } else {
                false
            }

            val startDestination = when {
                !isValid -> Screen.Login.route
                SharedPreferencesHelper.isNewUser(this@MainActivity) -> Screen.Onboarding.route
                !SharedPreferencesHelper.isOnboardingCompleted(this@MainActivity) -> Screen.Onboarding.route
                else -> Screen.Dashboard.route
            }

            runOnUiThread {
                setContent {
                    KaliumTheme {
                        AppNavigation(
                            context = this@MainActivity,
                            startDestination = startDestination,
                            userRepository = userRepository
                        )
                    }
                }
            }
        }
    }
}