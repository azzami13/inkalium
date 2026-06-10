package com.example.kaliumapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaliumapp.data.database.entities.LocationPoint
import com.example.kaliumapp.data.database.entities.WalkSession
import com.example.kaliumapp.data.repository.TrackingRepository
import com.example.kaliumapp.model.FitnessActivity
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityTrackingUiState(
    val selectedActivity: FitnessActivity? = null,
    val isTracking: Boolean = false,
    val startedAt: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Float = 0f,
    val speedMetersPerSecond: Float = 0f,
    val calories: Double = 0.0,
    val path: List<LatLng> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class ActivityTrackingViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityTrackingUiState())
    val uiState: StateFlow<ActivityTrackingUiState> = _uiState.asStateFlow()

    fun selectActivity(activity: FitnessActivity) {
        _uiState.value = ActivityTrackingUiState(selectedActivity = activity)
    }

    fun startTracking() {
        val activity = _uiState.value.selectedActivity ?: return
        _uiState.value = ActivityTrackingUiState(
            selectedActivity = activity,
            isTracking = true,
            startedAt = System.currentTimeMillis()
        )
    }

    fun updateTracking(
        elapsedSeconds: Long,
        distanceMeters: Float,
        speedMetersPerSecond: Float,
        calories: Double,
        latitude: Double?,
        longitude: Double?
    ) {
        val current = _uiState.value
        val nextPath = if (current.selectedActivity?.needsMap == true && latitude != null && longitude != null) {
            val point = LatLng(latitude, longitude)
            if (current.path.lastOrNull() != point) current.path + point else current.path
        } else {
            current.path
        }

        _uiState.value = current.copy(
            elapsedSeconds = elapsedSeconds,
            distanceMeters = distanceMeters,
            speedMetersPerSecond = speedMetersPerSecond,
            calories = calories,
            path = nextPath
        )
    }

    fun tickStaticActivity() {
        val current = _uiState.value
        val activity = current.selectedActivity ?: return
        if (!current.isTracking || activity.needsMap) return

        val elapsedSeconds = ((System.currentTimeMillis() - current.startedAt) / 1000L).coerceAtLeast(0L)
        val calories = activity.met * DEFAULT_WEIGHT_KG * (elapsedSeconds / 3600.0)
        _uiState.value = current.copy(
            elapsedSeconds = elapsedSeconds,
            calories = calories
        )
    }

    fun stopTracking(userId: Int) {
        val current = _uiState.value
        val activity = current.selectedActivity
        if (!current.isTracking || activity == null) return

        if (activity.needsMap && current.path.isNotEmpty()) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val session = WalkSession(
                    userId = userId,
                    startTime = current.startedAt,
                    endTime = now,
                    totalDistance = current.distanceMeters,
                    avgSpeed = current.speedMetersPerSecond
                )
                val points = current.path.map {
                    LocationPoint(
                        sessionId = 0,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = now
                    )
                }
                trackingRepository.saveSession(session, points)
            }
        }

        _uiState.value = current.copy(isTracking = false, message = "Aktivitas selesai: ${current.calories.toInt()} kcal")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        private const val DEFAULT_WEIGHT_KG = 65.0
    }
}
