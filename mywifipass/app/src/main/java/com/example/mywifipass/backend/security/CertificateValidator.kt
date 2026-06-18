/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.backend.security

import android.util.Log
import java.security.cert.X509Certificate
import java.util.*

/**
 * Validates X.509 certificates for self-hosted environments.
 * Ensures certificates are properly formatted and not expired.
 */
object CertificateValidator {
    
    private const val TAG = "CertificateValidator"
    
    /**
     * Validates a certificate for use in the application.
     *
     * @param certificate X509Certificate to validate
     * @return ValidationResult with details
     */
    fun validateCertificate(certificate: X509Certificate): CertificateValidationResult {
        return try {
            // Check certificate is currently valid (not expired)
            certificate.checkValidity()
            
            // Get certificate validity dates
            val notBefore = certificate.notBefore
            val notAfter = certificate.notAfter
            val now = Date()
            
            // Calculate days until expiration
            val daysUntilExpiry = ((notAfter.time - now.time) / (1000 * 60 * 60 * 24)).toInt()
            
            // Log certificate info for debugging
            Log.d(TAG, "Certificate CN: ${certificate.subjectDN}")
            Log.d(TAG, "Valid from: $notBefore")
            Log.d(TAG, "Valid until: $notAfter")
            Log.d(TAG, "Days until expiry: $daysUntilExpiry")
            
            // Warn if certificate expires soon (within 30 days)
            if (daysUntilExpiry < 30) {
                Log.w(TAG, "Certificate expires in $daysUntilExpiry days - consider renewal")
            }
            
            // Check that certificate is not yet valid (not in future)
            if (now.before(notBefore)) {
                return CertificateValidationResult.Invalid(
                    "Certificate is not yet valid. Valid from: $notBefore"
                )
            }
            
            CertificateValidationResult.Valid(
                subject = certificate.subjectDN?.toString() ?: "Unknown",
                issuer = certificate.issuerDN?.toString() ?: "Unknown",
                notBefore = notBefore,
                notAfter = notAfter,
                daysUntilExpiry = daysUntilExpiry
            )
        } catch (e: Exception) {
            when (e) {
                is java.security.cert.CertificateExpiredException -> {
                    CertificateValidationResult.Invalid("Certificate has expired: ${e.message}")
                }
                is java.security.cert.CertificateNotYetValidException -> {
                    CertificateValidationResult.Invalid("Certificate is not yet valid: ${e.message}")
                }
                else -> {
                    CertificateValidationResult.Invalid("Certificate validation error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Validates a chain of certificates.
     * Useful for validating CA + Client certificate chains.
     */
    fun validateCertificateChain(certificates: List<X509Certificate>): CertificateChainValidationResult {
        return try {
            if (certificates.isEmpty()) {
                return CertificateChainValidationResult.Invalid("Certificate chain is empty")
            }
            
            val validationResults = mutableListOf<CertificateValidationResult>()
            var anyExpiringSoon = false
            
            for ((index, cert) in certificates.withIndex()) {
                val result = validateCertificate(cert)
                validationResults.add(result)
                
                if (result is CertificateValidationResult.Valid && result.daysUntilExpiry < 30) {
                    anyExpiringSoon = true
                    Log.w(TAG, "Certificate at index $index expires in ${result.daysUntilExpiry} days")
                }
                
                if (result is CertificateValidationResult.Invalid) {
                    return CertificateChainValidationResult.Invalid(
                        "Certificate at index $index is invalid: ${result.reason}"
                    )
                }
            }
            
            CertificateChainValidationResult.Valid(
                certificateCount = certificates.size,
                expirationWarnings = if (anyExpiringSoon) 
                    "One or more certificates expire within 30 days" else null
            )
        } catch (e: Exception) {
            CertificateChainValidationResult.Invalid("Error validating certificate chain: ${e.message}")
        }
    }
}

/**
 * Result type for certificate validation
 */
sealed class CertificateValidationResult {
    data class Valid(
        val subject: String,
        val issuer: String,
        val notBefore: Date,
        val notAfter: Date,
        val daysUntilExpiry: Int
    ) : CertificateValidationResult()
    
    data class Invalid(val reason: String) : CertificateValidationResult()
}

/**
 * Result type for certificate chain validation
 */
sealed class CertificateChainValidationResult {
    data class Valid(
        val certificateCount: Int,
        val expirationWarnings: String?
    ) : CertificateChainValidationResult()
    
    data class Invalid(val reason: String) : CertificateChainValidationResult()
}
