package app.mywifipass.model.repository

import android.content.Context
import app.mywifipass.model.data.LoginCredentials
import app.mywifipass.backend.api_petitions.loginPetition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    // SharedPreferences keys
    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val TOKEN_KEY = "auth_token"
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
                    onSuccess = { token ->
                        authToken = token
                        saveToken(token)
                    },
                    onError = { error ->
                        loginError = error
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
     * Saves authentication token to SharedPreferences
     * @param token Authentication token to save
     */
    private fun saveToken(token: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(TOKEN_KEY, token)
            apply()
        }
    }
    
    /**
     * Retrieves stored authentication token
     * @return Stored token or null if not found
     */
    fun getStoredToken(): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(TOKEN_KEY, null)
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
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(TOKEN_KEY)
            apply()
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
