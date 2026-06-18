package app.mywifipass.fido2

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

/**
 * Helper functions for Android Credentials API (FIDO2)
 * These require Activity instead of Context as per API requirements
 */

/**
 * Invokes the Android CredentialManager to retrieve a FIDO2 passkey credential
 * to authenticate against an RP (Relying Party).
 * 
 * @param activity Activity scope required by the CredentialManager prompt UI
 * @param request Configuration container carrying the challenge and options
 * @return The user's biometric approval response containing the signature
 */
suspend fun getCredential(
    activity: Activity,
    request: GetCredentialRequest
): GetCredentialResponse {
    val credentialManager = CredentialManager.create(activity)
    return credentialManager.getCredential(activity, request)
}
