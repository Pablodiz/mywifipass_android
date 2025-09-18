package app.mywifipass.controller

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

import app.mywifipass.model.data.QrData
import app.mywifipass.model.repository.AuthRepository
import app.mywifipass.backend.api_petitions.checkAttendee
import app.mywifipass.backend.api_petitions.authorizeAttendee

/**
 * AdminController handles admin-specific business logic
 * Coordinates QR validation, attendee authorization, and admin authentication
 */
class AdminController(private val context: Context) {
    
    private val authRepository = AuthRepository(context)
    
    /**
     * Data class to represent attendee validation result
     */
    data class AttendeeValidationResult(
        val message: String,
        val authorizeUrl: String,
        val isSuccess: Boolean
    )
    
    /**
     * Validates a QR code for attendee verification
     * @param qrCode QR code string to validate
     * @return Result containing AttendeeValidationResult or error
     */
    suspend fun validateQR(qrCode: String): Result<AttendeeValidationResult> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AdminController", "Validating QR code")
                
                // Parse QR code to get validation URL
                val parseResult = parseQRCode(qrCode)
                if (parseResult.isFailure) {
                    return@withContext parseResult.map { 
                        AttendeeValidationResult("", "", false) 
                    }
                }
                
                val qrData = parseResult.getOrNull()!!
                val endpoint = qrData.validation_url
                
                // Get stored auth token
                val token = authRepository.getStoredToken()
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Authentication token is missing"))
                }
                
                // Validate attendee using API
                var validationResult: AttendeeValidationResult? = null
                var errorMessage: String? = null
                
                checkAttendee(
                    endpoint = endpoint,
                    token = token,
                    context = context,
                    onSuccess = { message, authorizeUrl -> 
                        validationResult = AttendeeValidationResult(
                            message = message,
                            authorizeUrl = authorizeUrl,
                            isSuccess = true
                        )
                        Log.d("AdminController", "QR validation successful: $message")
                    },
                    onError = { error -> 
                        errorMessage = error
                        Log.e("AdminController", "QR validation failed: $error")
                    }
                )
                
                // Return result
                errorMessage?.let {
                    Result.success(AttendeeValidationResult(
                        message = it,
                        authorizeUrl = "",
                        isSuccess = false
                    ))
                } ?: validationResult?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Unknown validation error"))
                
            } catch (e: Exception) {
                Log.e("AdminController", "Error validating QR: ${e.message}")
                Result.failure(Exception("QR validation failed: ${e.message}"))
            }
        }
    }
    
    /**
     * Authorizes an attendee using the authorization URL
     * @param authorizeUrl URL for attendee authorization
     * @return Result containing success message or error
     */
    suspend fun authorizeAttendee(authorizeUrl: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AdminController", "Authorizing attendee")
                
                if (authorizeUrl.isEmpty()) {
                    return@withContext Result.failure(Exception("Authorization URL is empty"))
                }
                
                // Get stored auth token
                val token = authRepository.getStoredToken()
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Authentication token is missing"))
                }
                
                // Authorize attendee using API
                var authorizationResult: String? = null
                var errorMessage: String? = null
                
                authorizeAttendee(
                    endpoint = authorizeUrl,
                    token = token,
                    context = context,
                    onSuccess = { message -> 
                        authorizationResult = message
                        Log.d("AdminController", "Attendee authorization successful: $message")
                    },
                    onError = { error -> 
                        errorMessage = error
                        Log.e("AdminController", "Attendee authorization failed: $error")
                    }
                )
                
                // Return result
                errorMessage?.let {
                    Result.failure(Exception(it))
                } ?: authorizationResult?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Unknown authorization error"))
                
            } catch (e: Exception) {
                Log.e("AdminController", "Error authorizing attendee: ${e.message}")
                Result.failure(Exception("Attendee authorization failed: ${e.message}"))
            }
        }
    }
    
    /**
     * Checks if admin has valid authentication token
     * @return true if valid token exists, false otherwise
     */
    fun hasValidToken(): Boolean {
        return try {
            val token = authRepository.getStoredToken()
            !token.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e("AdminController", "Error checking token validity: ${e.message}")
            false
        }
    }
    
    /**
     * Logs out the admin by clearing the authentication token
     */
    fun logout() {
        try {
            authRepository.clearToken()
            Log.d("AdminController", "Admin logged out successfully")
        } catch (e: Exception) {
            Log.e("AdminController", "Error during logout: ${e.message}")
        }
    }
    
    /**
     * Validates QR code format and structure
     * @param qrCode QR code string to validate
     * @return Result containing success message or error
     */
    fun validateQRFormat(qrCode: String): Result<String> {
        return try {
            if (qrCode.isEmpty()) {
                return Result.failure(Exception("QR code is empty"))
            }
            
            // Try to parse as QrData to validate format
            val parseResult = parseQRCode(qrCode)
            if (parseResult.isFailure) {
                return parseResult.map { "QR code format is valid" }
            }
            
            val qrData = parseResult.getOrNull()!!
            if (qrData.validation_url.isEmpty()) {
                return Result.failure(Exception("QR code missing validation URL"))
            }
            
            Result.success("QR code format is valid")
            
        } catch (e: Exception) {
            Log.e("AdminController", "QR format validation error: ${e.message}")
            Result.failure(Exception("Invalid QR code format: ${e.message}"))
        }
    }
    
    // Private helper methods
    
    /**
     * Parses QR code string to QrData object
     * @param qrCode QR code string
     * @return Result containing QrData or error
     */
    private fun parseQRCode(qrCode: String): Result<QrData> {
        return try {
            val qrData = Json.decodeFromString<QrData>(qrCode)
            
            if (qrData.validation_url.isNotEmpty()) {
                Result.success(qrData)
            } else {
                Result.failure(Exception("QR code contains empty validation URL"))
            }
            
        } catch (e: Exception) {
            Result.failure(Exception("Invalid QR code format: ${e.message}"))
        }
    }
}
