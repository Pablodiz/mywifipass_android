package app.mywifipass.controller

import android.content.Context
import app.mywifipass.model.data.LoginCredentials
import app.mywifipass.model.data.QrLoginCredentials
import app.mywifipass.model.repository.AuthRepository
import kotlinx.serialization.json.Json
import app.mywifipass.R

class LoginController(private val context: Context) {
    
    private val authRepository = AuthRepository(context)
    
    /**
     * Validates and processes user login with credentials
     * @param credentials User login credentials
     * @return Result containing success message or error
     */
    suspend fun login(credentials: LoginCredentials): Result<String> {
        return try {
            // Validate credentials before attempting login
            val validationResult = validateCredentials(credentials)
            if (validationResult.isFailure) {
                return validationResult
            }
            
            // Attempt login through repository
            val loginResult = authRepository.login(credentials)
            
            loginResult.fold(
                onSuccess = { _ ->
                    Result.success(context.getString(R.string.login_successful))
                },
                onFailure = { exception ->
                    Result.failure(Exception("${context.getString(R.string.login_failed)}: ${exception.message}"))
                }
            )
            
        } catch (e: Exception) {
            Result.failure(Exception("${context.getString(R.string.login_error)}: ${e.message}"))
        }
    }
    
    /**
     * Processes QR code login
     * @param qrCode QR code string containing login credentials
     * @return Result containing success message or error
     */
    suspend fun loginWithQR(qrCode: String): Result<String> {
        return try {
            // Parse QR code to credentials
            val qrCredentials = parseQRCredentials(qrCode)
            if (qrCredentials.isFailure) {
                return Result.failure(qrCredentials.exceptionOrNull()!!)
            }
            
            val credentials = qrCredentials.getOrNull()!!
            
            // Convert QR credentials to login credentials
            val loginCredentials = LoginCredentials(
                url = credentials.url,
                login = credentials.username,
                pwd = credentials.token, 
                usePassword = false
            )
            
            // Perform login
            login(loginCredentials)
            
        } catch (e: Exception) {
            Result.failure(Exception("${context.getString(R.string.qr_login_error)}: ${e.message}"))
        }
    }
    
    /**
     * Checks if user has a stored authentication token
     * @return true if user has valid token, false otherwise
     */
    fun hasStoredToken(): Boolean {
        return authRepository.hasValidToken()
    }
    
    /**
     * Gets the stored authentication token
     * @return stored token or null
     */
    fun getStoredToken(): String? {
        return authRepository.getStoredToken()
    }
    
    /**
     * Logs out the user by clearing stored authentication data
     */
    fun logout() {
        authRepository.logout()
    }
    
    /**
     * Validates login credentials
     * @param credentials Credentials to validate
     * @return Result indicating validation success or failure
     */
    private fun validateCredentials(credentials: LoginCredentials): Result<String> {
        return when {
            credentials.url.isBlank() -> 
                Result.failure(Exception(context.getString(R.string.url_cannot_be_empty)))
            credentials.login.isBlank() -> 
                Result.failure(Exception(context.getString(R.string.username_cannot_be_empty)))
            credentials.pwd.isBlank() -> 
                Result.failure(Exception(context.getString(R.string.password_cannot_be_empty)))
            !isValidUrl(credentials.url) -> 
                Result.failure(Exception(context.getString(R.string.invalid_url_format)))
            else -> 
                Result.success(context.getString(R.string.validation_passed))
        }
    }
    
    /**
     * Parses QR code string to QR credentials
     * @param qrCode QR code string
     * @return Result containing QrLoginCredentials or error
     */
    private fun parseQRCredentials(qrCode: String): Result<QrLoginCredentials> {
        return try {
            val qrCredentials = Json.decodeFromString<QrLoginCredentials>(qrCode)
            
            if (qrCredentials.isNotEmpty()) {
                Result.success(qrCredentials)
            } else {
                Result.failure(Exception(context.getString(R.string.qr_code_empty_credentials)))
            }
            
        } catch (e: Exception) {
            Result.failure(Exception("${context.getString(R.string.invalid_qr_code_format)}: ${e.message}"))
        }
    }
    
    /**
     * Basic URL validation
     * @param url URL to validate
     * @return true if URL format is valid
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
}