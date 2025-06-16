package com.example.kaliumapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HistoryItem(
    val activityName: String,
    val met: Double,
    val duration: Int, // Dalam menit
    val calories: Double, // Dalam kkal
    val timestamp: String // Format tanggal dan waktu
) {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun create(activity: ActivityItem, duration: Int, calories: Double): HistoryItem {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val timestamp = LocalDateTime.now().format(formatter)
            return HistoryItem(
                activityName = activity.name,
                met = activity.met,
                duration = duration,
                calories = calories,
                timestamp = timestamp
            )
        }
    }
}