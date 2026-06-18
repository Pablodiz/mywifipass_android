/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.backend.security

import android.net.Uri
import android.util.Patterns

/**
 * Validates URLs for security purposes in a self-hosted multi-tenant environment.
 * Allows multiple server instances while maintaining security standards.
 */
object UrlValidator {
    
    /**
     * Validates if a URL is properly formatted and safe to use.
     * Supports both HTTP (for development/self-hosted) and HTTPS (recommended).
     *
     * @param urlString URL to validate
     * @return True if URL is valid, false otherwise
     */
    fun isValidUrl(urlString: String): Boolean {
        return try {
            // Check if URL is not empty
            if (urlString.isBlank()) return false
            
            // Parse URI
            val uri = Uri.parse(urlString)
            
            // Validate scheme - allow http and https only
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return false
            
            // Validate host is not empty
            val host = uri.host
            if (host.isNullOrBlank()) return false
            
            // Use Android's built-in pattern validation for additional check
            if (!Patterns.WEB_URL.matcher(urlString).matches()) return false
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates URLs from QR codes or deep links.
     * Ensures they meet security requirements for the self-hosted environment.
     *
     * @param urlString URL to validate
     * @param allowedSchemes Allowed URL schemes (default: http, https)
     * @return Validation result with detailed error if failed
     */
    fun validateUrlFromSource(
        urlString: String,
        allowedSchemes: List<String> = listOf("http", "https")
    ): UrlValidationResult {
        return try {
            // Check if URL is not empty
            if (urlString.isBlank()) {
                return UrlValidationResult.Invalid("URL cannot be empty")
            }
            
            // Parse URI
            val uri = Uri.parse(urlString)
            
            // Validate scheme
            val scheme = uri.scheme?.lowercase()
            if (scheme == null || !allowedSchemes.contains(scheme)) {
                return UrlValidationResult.Invalid("Unsupported URL scheme: $scheme. Allowed: ${allowedSchemes.joinToString(", ")}")
            }
            
            // Validate host
            val host = uri.host
            if (host.isNullOrBlank()) {
                return UrlValidationResult.Invalid("URL host is missing or invalid")
            }
            
            // Check for local network addresses (self-hosted typically runs here)
            // Don't block localhost/192.168.x.x etc - needed for self-hosted
            // but do log them for debugging
            if (isLocalAddress(host)) {
                android.util.Log.d("UrlValidator", "URL points to local address: $host (acceptable for self-hosted)")
            }
            
            // Validate that host doesn't contain suspicious patterns
            if (containsSuspiciousPatterns(host)) {
                return UrlValidationResult.Invalid("URL contains suspicious patterns")
            }
            
            // Validate port if specified
            if (uri.port > 0) {
                if (uri.port !in 1..65535) {
                    return UrlValidationResult.Invalid("Invalid port number: ${uri.port}")
                }
            }
            
            UrlValidationResult.Valid(urlString)
        } catch (e: Exception) {
            UrlValidationResult.Invalid("Invalid URL format: ${e.message}")
        }
    }
    
    /**
     * Checks if a hostname is a local/private network address.
     * Used for self-hosted environments.
     */
    private fun isLocalAddress(host: String): Boolean {
        return host.equals("localhost", ignoreCase = true)
                || host.startsWith("127.")
                || host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.startsWith("172.")
    }
    
    /**
     * Checks for suspicious patterns in URLs that could indicate attacks.
     */
    private fun containsSuspiciousPatterns(host: String): Boolean {
        // Check for encoded characters that could bypass validation
        if (host.contains("%")) return true
        
        // Check for suspicious UNICODE characters
        if (host.any { it.code > 127 }) return true
        
        // Check for double dots (directory traversal)
        if (host.contains("..")) return true
        
        return false
    }
}

/**
 * Result type for URL validation
 */
sealed class UrlValidationResult {
    data class Valid(val url: String) : UrlValidationResult()
    data class Invalid(val reason: String) : UrlValidationResult()
}
