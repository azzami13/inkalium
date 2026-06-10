package com.example.kaliumapp.model

data class FitnessActivity(
    val name: String,
    val met: Double,
    val needsMap: Boolean
)

object FitnessActivities {
    val items = listOf(
        FitnessActivity("Jalan Santai", 2.8, true),
        FitnessActivity("Jalan Cepat", 3.9, true),
        FitnessActivity("Lari Ringan", 8.3, true),
        FitnessActivity("Lari Sedang", 9.8, true),
        FitnessActivity("Lari Kencang", 11.0, true),
        FitnessActivity("Bersepeda Santai", 4.0, true),
        FitnessActivity("Bersepeda Sedang", 6.8, true),
        FitnessActivity("Hiking", 6.3, true),
        FitnessActivity("Renang Santai", 5.8, false),
        FitnessActivity("Renang Sedang", 8.0, false),
        FitnessActivity("Senam Aerobik", 5.0, false),
        FitnessActivity("Yoga", 2.5, false),
        FitnessActivity("Zumba", 6.0, false),
        FitnessActivity("Bulu Tangkis", 5.5, false),
        FitnessActivity("Sepak Bola", 7.0, false),
        FitnessActivity("Basket", 6.5, false),
        FitnessActivity("Voli", 4.0, false),
        FitnessActivity("Tenis Meja", 4.0, false),
        FitnessActivity("Membersihkan Rumah", 3.5, false),
        FitnessActivity("Naik Tangga", 8.8, false)
    )
}
