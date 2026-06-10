// SharedPreferencesHelper.kt - Updated dengan clear all functions
package com.example.kaliumapp.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SharedPreferencesHelper {

    private const val PREF_NAME = "kalium_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_IS_NEW_USER = "is_new_user"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_LAST_SYNC_DATE = "last_sync_date"
    private const val KEY_LAST_LOGIN_AT = "last_login_at"
    private const val KEY_LAST_SESSION_ACCESS_AT = "last_session_access_at"
    private const val SESSION_VALIDITY_MILLIS = 30L * 24 * 60 * 60 * 1000

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        val now = System.currentTimeMillis()
        getPreferences(context).edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_LAST_LOGIN_AT, now)
            .putLong(KEY_LAST_SESSION_ACCESS_AT, now)
            .apply()
        Log.d("SharedPreferencesHelper", "Token saved")
    }

    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    fun clearToken(context: Context) {
        getPreferences(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LAST_LOGIN_AT)
            .remove(KEY_LAST_SESSION_ACCESS_AT)
            .apply()
        Log.d("SharedPreferencesHelper", "Token cleared")
    }

    fun saveUserBasicInfo(context: Context, id: Int, email: String, username: String?) {
        getPreferences(context).edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_EMAIL, email)
            username?.let { putString(KEY_USERNAME, it) }
        }.apply()
        Log.d("SharedPreferencesHelper", "User basic info saved: id=$id, email=$email")
    }

    fun getUserId(context: Context): Int {
        return getPreferences(context).getInt(KEY_USER_ID, -1)
    }

    fun getEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_EMAIL, null)
    }

    fun getUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USERNAME, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return isValidSession(context)
    }

    fun markSessionAccessed(context: Context) {
        if (!getToken(context).isNullOrEmpty()) {
            getPreferences(context).edit()
                .putLong(KEY_LAST_SESSION_ACCESS_AT, System.currentTimeMillis())
                .apply()
        }
    }

    fun isSessionExpired(context: Context): Boolean {
        val lastAccess = getPreferences(context).getLong(KEY_LAST_SESSION_ACCESS_AT, 0L)
        if (lastAccess == 0L) return false

        val expired = System.currentTimeMillis() - lastAccess > SESSION_VALIDITY_MILLIS
        Log.d("SharedPreferencesHelper", "Session expired: $expired")
        return expired
    }

    fun setNewUser(context: Context, isNew: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_IS_NEW_USER, isNew).apply()
        Log.d("SharedPreferencesHelper", "New user status set: $isNew")
    }

    fun isNewUser(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_NEW_USER, false)
    }

    fun clearNewUser(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_IS_NEW_USER, false).apply()
        Log.d("SharedPreferencesHelper", "New user status cleared")
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        Log.d("SharedPreferencesHelper", "Onboarding completed status set: $completed")
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun saveLastSyncDate(context: Context, date: String) {
        getPreferences(context).edit().putString(KEY_LAST_SYNC_DATE, date).apply()
        Log.d("SharedPreferencesHelper", "Last sync date saved: $date")
    }

    fun getLastSyncDate(context: Context): String? {
        return getPreferences(context).getString(KEY_LAST_SYNC_DATE, null)
    }

    /**
     * Clear all SharedPreferences data - digunakan saat logout
     */
    fun clearAll(context: Context) {
        try {
            val editor = getPreferences(context).edit()
            editor.clear()
            editor.apply()
            Log.d("SharedPreferencesHelper", "All SharedPreferences data cleared successfully")
        } catch (e: Exception) {
            Log.e("SharedPreferencesHelper", "Error clearing SharedPreferences", e)
        }
    }

    /**
     * Clear hanya data sensitif (token dan session related) tapi keep user preferences
     */
    fun clearSessionData(context: Context) {
        try {
            val editor = getPreferences(context).edit()
            editor.remove(KEY_TOKEN)
            editor.remove(KEY_USER_ID)
            editor.remove(KEY_IS_NEW_USER)
            editor.remove(KEY_LAST_SYNC_DATE)
            editor.remove(KEY_LAST_LOGIN_AT)
            editor.remove(KEY_LAST_SESSION_ACCESS_AT)
            editor.apply()
            Log.d("SharedPreferencesHelper", "Session data cleared successfully")
        } catch (e: Exception) {
            Log.e("SharedPreferencesHelper", "Error clearing session data", e)
        }
    }

    /**
     * Debug function untuk melihat semua data yang tersimpan
     */
    fun debugPrintAll(context: Context) {
        try {
            val prefs = getPreferences(context)
            val allEntries = prefs.all
            Log.d("SharedPreferencesHelper", "=== SharedPreferences Debug ===")
            for ((key, value) in allEntries) {
                Log.d("SharedPreferencesHelper", "$key: $value")
            }
            Log.d("SharedPreferencesHelper", "=== End Debug ===")
        } catch (e: Exception) {
            Log.e("SharedPreferencesHelper", "Error debugging SharedPreferences", e)
        }
    }

    /**
     * Check apakah user sudah login dan data masih valid
     */
    fun isValidSession(context: Context): Boolean {
        val token = getToken(context)

        val hasToken = !token.isNullOrEmpty()
        val isValid = hasToken && !isSessionExpired(context)
        Log.d("SharedPreferencesHelper", "Session validation: $isValid (token=$hasToken)")

        return isValid
    }

    /**
     * Reset ke kondisi fresh install
     */
    fun resetToFreshInstall(context: Context) {
        try {
            clearAll(context)
            Log.d("SharedPreferencesHelper", "App reset to fresh install state")
        } catch (e: Exception) {
            Log.e("SharedPreferencesHelper", "Error resetting app", e)
        }
    }
}
