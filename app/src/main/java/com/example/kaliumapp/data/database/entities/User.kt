package com.example.kaliumapp.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val hashedPassword: String,
    val username: String? = null,
    val role: String = "user",
    val umur: Int = 0,
    val jenisKelamin: String = "",
    var beratBadan: Double = 0.0,
    val tinggiBadan: Double = 0.0,
    val dibuatPada: Long = System.currentTimeMillis(),
    val profileImage: String? = null,
    val lastLogin: Long? = null,
    val loginCount: Int = 0,
    val isActive: Boolean = true
)