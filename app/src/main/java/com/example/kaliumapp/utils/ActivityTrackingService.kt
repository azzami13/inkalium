package com.example.kaliumapp.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.kaliumapp.MainActivity
import com.example.kaliumapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.roundToInt

class ActivityTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var activityName: String = "Aktivitas"
    private var met: Double = 3.5
    private var startTime: Long = 0L
    private var lastLocation: Location? = null
    private var distanceMeters: Float = 0f
    private var calories: Double = 0.0
    private var speedMetersPerSecond: Float = 0f
    private var needsMap: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                handleLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        activityName = intent?.getStringExtra(EXTRA_ACTIVITY_NAME) ?: activityName
        met = intent?.getDoubleExtra(EXTRA_MET, met) ?: met
        needsMap = intent?.getBooleanExtra(EXTRA_NEEDS_MAP, false) ?: false
        startTime = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (needsMap) {
            startLocationUpdates()
        } else {
            sendUpdate()
            updateNotification()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sendBroadcast(Intent(ACTION_TRACKING_STOPPED))
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun handleLocation(location: Location) {
        val previous = lastLocation
        if (previous != null && location.accuracy <= 50f) {
            val delta = previous.distanceTo(location)
            if (delta in 1f..150f) {
                distanceMeters += delta
            }
        }

        lastLocation = location
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
        speedMetersPerSecond = distanceMeters / elapsedSeconds
        calories = calculateCalories(elapsedSeconds)

        sendUpdate(location)
        updateNotification()
    }

    private fun calculateCalories(elapsedSeconds: Long): Double {
        val hours = elapsedSeconds / 3600.0
        return met * DEFAULT_WEIGHT_KG * hours
    }

    private fun sendUpdate(location: Location? = lastLocation) {
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L)
        val intent = Intent(ACTION_TRACKING_UPDATE).apply {
            putExtra(EXTRA_ACTIVITY_NAME, activityName)
            putExtra(EXTRA_DISTANCE_METERS, distanceMeters)
            putExtra(EXTRA_SPEED_MPS, speedMetersPerSecond)
            putExtra(EXTRA_CALORIES, calories)
            putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
            location?.let {
                putExtra(EXTRA_LATITUDE, it.latitude)
                putExtra(EXTRA_LONGITUDE, it.longitude)
                putExtra(EXTRA_ACCURACY, it.accuracy)
            }
        }
        sendBroadcast(intent)
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ActivityTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = if (distanceMeters >= 1000f) {
            "%.2f km".format(distanceMeters / 1000f)
        } else {
            "${distanceMeters.roundToInt()} m"
        }
        val speedText = "%.1f km/j".format(speedMetersPerSecond * 3.6f)
        val caloriesText = "${calories.roundToInt()} kcal"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$activityName sedang berjalan")
            .setContentText("$distanceText | $speedText | $caloriesText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Jarak $distanceText, kecepatan $speedText, kalori $caloriesText"))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Monitoring jarak, kecepatan, dan kalori saat aktivitas berlangsung"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_TRACKING_UPDATE = "com.example.kaliumapp.TRACKING_UPDATE"
        const val ACTION_TRACKING_STOPPED = "com.example.kaliumapp.TRACKING_STOPPED"
        const val ACTION_STOP = "com.example.kaliumapp.STOP_TRACKING"

        const val EXTRA_ACTIVITY_NAME = "activity_name"
        const val EXTRA_MET = "met"
        const val EXTRA_NEEDS_MAP = "needs_map"
        const val EXTRA_DISTANCE_METERS = "distance_meters"
        const val EXTRA_SPEED_MPS = "speed_mps"
        const val EXTRA_CALORIES = "calories"
        const val EXTRA_ELAPSED_SECONDS = "elapsed_seconds"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ACCURACY = "accuracy"

        private const val CHANNEL_ID = "activity_tracking_channel"
        private const val NOTIFICATION_ID = 4242
        private const val DEFAULT_WEIGHT_KG = 65.0
    }
}
