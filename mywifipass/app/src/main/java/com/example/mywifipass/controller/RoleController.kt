package app.mywifipass.controller

import android.content.Context
import app.mywifipass.model.repository.AuthRepository

/**
 * RoleController handles role-based logic and navigation decisions.
 * This controller determines user roles and manages role-based application flow.
 */
class RoleController(private val context: Context) {
    
    private val authRepository = AuthRepository(context)
    
    /**
     * Checks if user has a stored authentication token
     * @return true if user has valid token, false otherwise
     */
    fun hasStoredAuthToken(): Boolean {
        val token = authRepository.getStoredToken()
        return !token.isNullOrEmpty()
    }
    
    /**
     * Determines the user's role based on authentication status
     * @return String representing the user role ("guest", "user", "admin")
     */
    fun getUserRole(): String {
        return if (hasStoredAuthToken()) {
            // For now, we'll assume authenticated users are regular users
            // In a more complex system, this could check token claims or make API calls
            "user"
        } else {
            "guest"
        }
    }
    
    /**
     * Determines if user should be redirected to login
     * @return true if user needs to login, false otherwise
     */
    fun requiresLogin(): Boolean {
        return !hasStoredAuthToken()
    }
    
    /**
     * Determines if user has admin privileges
     * @return true if user is admin, false otherwise
     */
    fun isAdmin(): Boolean {
        // This could be enhanced to check specific admin roles from token or API
        return hasStoredAuthToken() // Simplified: any authenticated user can access admin for now
    }
    
    /**
     * Gets the appropriate destination activity based on user role
     * @return String representing the destination ("login", "main", "admin")
     */
    fun getDestinationForRole(): String {
        return when {
            requiresLogin() -> "login"
            isAdmin() -> "main" // Could be "admin" if there's a specific admin home
            else -> "main"
        }
    }
    
    /**
     * Clears user session and resets role
     */
    fun logout() {
        authRepository.clearToken()
    }
}
