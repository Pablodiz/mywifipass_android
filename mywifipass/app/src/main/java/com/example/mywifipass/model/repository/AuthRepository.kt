/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.model.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.mywifipass.model.data.LoginCredentials
import app.mywifipass.backend.api_petitions.loginPetition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    // EncryptedSharedPreferences keys and initialization
    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val TOKEN_KEY = "auth_token"
        
        private fun getEncryptedSharedPreferences(context: Context) =
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
    }
    
    /**
     * Performs user login with the provided credentials
     * @param credentials User login credentials (url, username, password)
     * @return Result containing auth token on success or exception on failure
     */
    suspend fun login(credentials: LoginCredentials): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var authToken = ""
                var loginError: String? = null
                
                loginPetition(
                    url = credentials.url,
                    login = credentials.login,
                    pwd = credentials.pwd,
                    usePassword = credentials.usePassword,
                    context = context,
                    onSuccess = { token ->
                        authToken = token
                        saveToken(token)
                    },
                    onError = { apiResult ->
                        loginError = apiResult.message
                    }
                )
                
                if (loginError != null) {
                    Result.failure(Exception(loginError))
                } else {
                    Result.success(authToken)
                }
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Saves authentication token to encrypted SharedPreferences
     * @param token Authentication token to save
     */
    private fun saveToken(token: String) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        with(encryptedPrefs.edit()) {
            putString(TOKEN_KEY, token)
            apply()
        }
    }
    
    /**
     * Retrieves stored authentication token from encrypted storage
     * @return Stored token or null if not found
     */
    fun getStoredToken(): String? {
        return try {
            val encryptedPrefs = getEncryptedSharedPreferences(context)
            encryptedPrefs.getString(TOKEN_KEY, null)
        } catch (e: Exception) {
            // If decryption fails (e.g., corrupted data), clear and return null
            clearToken()
            null
        }
    }
    
    /**
     * Checks if user has a valid stored token
     * @return true if token exists, false otherwise
     */
    fun hasValidToken(): Boolean {
        return getStoredToken() != null
    }
    
    /**
     * Clears the stored authentication token (logout)
     */
    fun clearToken() {
        try {
            val encryptedPrefs = getEncryptedSharedPreferences(context)
            with(encryptedPrefs.edit()) {
                remove(TOKEN_KEY)
                apply()
            }
        } catch (e: Exception) {
            // Silently fail if cannot access preferences
        }
    }
    
    /**
     * Clears all authentication data
     */
    fun logout() {
        clearToken()
        // Could add more cleanup logic here if needed
    }
}
