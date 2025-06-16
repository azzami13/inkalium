package com.example.kaliumapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.kaliumapp.model.ActivityItem
import com.example.kaliumapp.model.HistoryItem

class HitungKaloriViewModel : ViewModel() {
    var selectedActivity by mutableStateOf<ActivityItem?>(null)
    var duration by mutableStateOf("")
    var result by mutableStateOf("")
    var historyList by mutableStateOf<List<HistoryItem>>(emptyList())

    private val beratBadan = 65.0

    val aktivitasList = listOf(
        ActivityItem("Jalan Santai (4 km/jam)", 2.8),
        ActivityItem("Jalan Cepat (5.5 km/jam)", 3.9),
        ActivityItem("Lari Ringan (8 km/jam)", 8.3),
        ActivityItem("Lari Sedang (9.5 km/jam)", 9.8),
        ActivityItem("Lari Kencang (11 km/jam)", 11.0),
        ActivityItem("Bersepeda Santai (10-12 km/jam)", 4.0),
        ActivityItem("Bersepeda Sedang (16-19 km/jam)", 6.8),
        ActivityItem("Renang Gaya Bebas Santai", 5.8),
        ActivityItem("Renang Gaya Bebas Sedang", 8.0),
        ActivityItem("Senam Aerobik", 5.0),
        ActivityItem("Yoga", 2.5),
        ActivityItem("Zumba", 6.0),
        ActivityItem("Bulu Tangkis Rekreasi", 5.5),
        ActivityItem("Bulu Tangkis Kompetitif", 7.0),
        ActivityItem("Sepak Bola Rekreasi", 7.0),
        ActivityItem("Sepak Bola Kompetitif", 10.0),
        ActivityItem("Basket", 6.5),
        ActivityItem("Voli", 4.0),
        ActivityItem("Tenis Meja", 4.0),
        ActivityItem("Tenis Lapangan (ganda)", 5.0),
        ActivityItem("Tenis Lapangan (tunggal)", 7.3),
        ActivityItem("Naik Gunung / Hiking", 6.3),
        ActivityItem("Bela Diri (Silat, Taekwondo, dll)", 10.0),
        ActivityItem("Menari Tradisional (enerjik)", 5.0),
        ActivityItem("Membersihkan Rumah (menyapu, mengepel)", 3.5),
        ActivityItem("Naik Tangga (perlahan)", 4.0),
        ActivityItem("Naik Tangga (cepat)", 8.8)
    )


    fun hitungKalori() {
        val durasiInt = duration.toIntOrNull()
        val met = selectedActivity?.met ?: return

        if (durasiInt != null && durasiInt > 0) {
            val durasiJam = durasiInt / 60.0
            val kalori = met * beratBadan * durasiJam
            result = "Kalori terbakar: %.2f kkal".format(kalori)

            selectedActivity?.let { activity ->
                val historyItem = HistoryItem.create(activity, durasiInt, kalori)
                historyList = historyList + historyItem
                println("HitungKaloriViewModel - HistoryList updated: $historyList")
            }
        } else {
            result = "Masukkan durasi yang valid"
        }
    }

    fun clearHistory() {
        historyList = emptyList()
        println("HitungKaloriViewModel - History cleared")
    }

    fun removeHistoryItem(index: Int) {
        if (index in historyList.indices) {
            historyList = historyList.toMutableList().apply { removeAt(index) }
            println("HitungKaloriViewModel - Removed history item at index $index")
        }
    }
}