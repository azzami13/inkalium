package com.example.kaliumapp.data.repository

import com.example.kaliumapp.data.database.dao.UserDao
import com.example.kaliumapp.data.database.entities.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalorieRepository @Inject constructor(
    private val userDao: UserDao
) {
    // Fungsi untuk menyimpan asupan kalori
    suspend fun saveCalorieIntake(calories: Int) {
        // Misalnya, kita simpan asupan kalori ke dalam User atau ke entitas lain
        val user = userDao.getUserById(1) // ambil user dengan id tertentu
        user?.let {
            // Update asupan kalori
            it.beratBadan += calories // contoh perhitungan (bisa disesuaikan)
            userDao.updateUser(it)
        }
    }

    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()
    }

    suspend fun getUserById(id: Int): User? {
        return userDao.getUserById(id)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
}