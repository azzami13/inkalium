package com.example.kaliumapp.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("umur") val umur: Int?,
    @SerializedName("jenis_kelamin") val jenisKelamin: String?,
    @SerializedName("berat_badan") val beratBadan: Double?,
    @SerializedName("tinggi_badan") val tinggiBadan: Double?,
    @SerializedName("profile_image") val profileImage: String?
)