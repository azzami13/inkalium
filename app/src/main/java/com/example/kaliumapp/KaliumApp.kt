package com.example.kaliumapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KaliumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this) // Inisialisasi ThreeTenABP
    }
}