package com.example.kaliumapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.navigation.AppNavigation
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.ui.theme.KaliumTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocationPermission && coarseLocationPermission && activityRecognitionPermission && notificationPermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun initializeApp() {
        val hasValidSession = SharedPreferencesHelper.isValidSession(this)

        if (hasValidSession) {
            SharedPreferencesHelper.markSessionAccessed(this)
        } else if (SharedPreferencesHelper.getToken(this) != null) {
            SharedPreferencesHelper.clearSessionData(this)
        }

        val startDestination = when {
            !hasValidSession -> Screen.Login.route
            SharedPreferencesHelper.isNewUser(this) -> Screen.Onboarding.route
            !SharedPreferencesHelper.isOnboardingCompleted(this) -> Screen.Onboarding.route
            else -> Screen.Dashboard.route
        }

        setContent {
            KaliumTheme {
                AppNavigation(
                    context = this@MainActivity,
                    startDestination = startDestination
                )
            }
        }
    }
}
