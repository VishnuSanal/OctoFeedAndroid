package com.vishnu.octofeed.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "github_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if EncryptedSharedPreferences fails
        context.getSharedPreferences("github_auth_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
    }

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveUserInfo(user: GitHubUser) {
        sharedPreferences.edit()
            .putString(KEY_USER_LOGIN, user.login)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_AVATAR, user.avatar_url)
            .apply()
    }

    fun getUserLogin(): String? {
        return sharedPreferences.getString(KEY_USER_LOGIN, null)
    }

    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    fun getUserAvatar(): String? {
        return sharedPreferences.getString(KEY_USER_AVATAR, null)
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    fun clearAuth() {
        sharedPreferences.edit().clear().apply()
    }
}

