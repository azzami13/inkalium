package com.example.kaliumapp.model

data class AuthRequest(
    val email: String,
    val password: String,
    val username: String? = null,
    val umur: Int? = null,
    val jenisKelamin: String? = null,
    val beratBadan: Double? = null,
    val tinggiBadan: Double? = null
)