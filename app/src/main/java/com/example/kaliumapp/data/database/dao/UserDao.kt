package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Query("UPDATE users SET lastLogin = :lastLogin, loginCount = loginCount + 1 WHERE id = :id")
    suspend fun updateLoginStats(id: Int, lastLogin: Long)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Int)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE id = :id")
    suspend fun checkUserExists(id: Int): Int

    @Query("SELECT * FROM users ORDER BY lastLogin DESC LIMIT 1")
    suspend fun getLastLoginUser(): User?

    @Query("UPDATE users SET isActive = 0")
    suspend fun deactivateAllUsers()

    @Query("UPDATE users SET isActive = 1 WHERE id = :id")
    suspend fun activateUser(id: Int)
}