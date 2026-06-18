package app.mywifipass.fido2

import android.app.Activity
import android.util.Log
import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.gson.Gson
import com.google.gson.JsonObject

import app.mywifipass.backend.api_petitions.fido2AuthenticateStart
import app.mywifipass.backend.api_petitions.fido2AuthenticateFinish

/**
 * Service for FIDO2 operations.
 * Handles communication between Android Credentials API and backend server 
 * for biometric/passkey authentication.
 * 
 * Uses complete URLs from backend - no endpoint construction in client code
 */
class Fido2Service {
    
    /**
     * Authenticate user with FIDO2 for network authorization
     * Returns success message on successful authentication
     * 
     * @param context Android context (automatically extracts Activity)
     * @param startUrl Complete URL for FIDO2 authentication start endpoint
     * @param finishUrl Complete URL for FIDO2 authentication finish endpoint
     * @param username User email for authentication
     * @param networkName Network name for logging
     * @return Success message
     */
    suspend fun authenticateForNetwork(
        context: Context,
        startUrl: String,
        finishUrl: String,
        username: String,
        networkName: String
    ): String {
        val activity = getActivityFromContext(context)
        return authenticateForNetwork(activity, startUrl, finishUrl, username, networkName, context)
    }
    
    /**
     * Authenticate user with FIDO2 for network authorization
     * Returns success message on successful authentication
     * 
     * @param activity Activity for credential request UI
     * @param startUrl Complete URL for FIDO2 authentication start endpoint
     * @param finishUrl Complete URL for FIDO2 authentication finish endpoint
     * @param username User email for authentication
     * @param networkName Network name for logging
     * @param context Android context
     * @return Success message
     */
    suspend fun authenticateForNetwork(
        activity: Activity,
        startUrl: String,
        finishUrl: String,
        username: String,
        networkName: String,
        context: Context
    ): String {
        try {
            Log.d(TAG, "Starting FIDO2 authentication for network: $networkName")

            // Step 1: Request authentication options from server.
            // session_id is non-null in discoverable mode (username is empty),
            // null in legacy email mode.
            val (optionsJson, sessionId) = getAuthenticationOptions(startUrl, username, context)

            // Step 2: Show biometric/passkey prompt via Android Credential Manager.
            val credentialResponseJson = getCredentialWithFido2(activity, optionsJson)

            // Step 3: Send assertion to server. In discoverable mode the session_id
            // is included instead of the email so the server can find the challenge.
            verifyAuthentication(finishUrl, username, credentialResponseJson, context, sessionId)

            Log.d(TAG, "FIDO2 authentication completed successfully for: $networkName")
            return success_message
            
        } catch (e: GetCredentialCancellationException) {
            Log.e(TAG, "FIDO2 authentication cancelled by user", e)
            throw Exception(cancelled_message)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "=== CREDENTIAL MANAGER ERROR ===")
            Log.e(TAG, "Exception class: ${e.javaClass.name}")
            Log.e(TAG, "Exception type:  ${e.type}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Cause: ${e.cause}")
            Log.e(TAG, "================================")
            throw Exception("Error al obtener credencial: [${e.type}] ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "FIDO2 authentication error: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Extracts an Activity from a given Context traversing the ContextWrapper chain.
     * Required because CredentialManager API strictly needs an Activity.
     *
     * @param context Android context
     * @return The underlying Activity
     * @throws Exception if no Activity is found
     */
    private fun getActivityFromContext(context: Context): Activity {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        throw Exception("Unable to get Activity from Context")
    }
    
    /**
     * Step 1: Request FIDO2 authentication options (challenge) from the backend.
     *
     * If [username] is empty the request is sent without an email, triggering
     * discoverable mode on the server: the response will contain
     * `allowCredentials=[]` and a `session_id` UUID.
     *
     * If [username] is non-empty the legacy email mode is used and no
     * `session_id` is returned.
     *
     * To re-enable the email mode from [MainController.validateWithFido2],
     * pass `network.user_email` as `username` instead of `""`.
     *
     * @param startUrl Endpoint URL for options request
     * @param username User email (empty string for discoverable mode)
     * @param context Android context
     * @return Pair of (options JSON for Credential Manager, session_id or null)
     */
    private suspend fun getAuthenticationOptions(
        startUrl: String,
        username: String,
        context: Context
    ): Pair<String, String?> {
        val modeLabel = if (username.isNotBlank()) "email" else "discoverable"
        Log.d(TAG, "Requesting authentication options (mode=$modeLabel)")
        val result = fido2AuthenticateStart(startUrl, username, context)
        val (optionsJson, sessionId) = result
        if (optionsJson.isBlank()) {
            throw Exception("Empty authentication options from server")
        }
        Log.d(TAG, "Authentication options received, discoverable=${sessionId != null}")
        return Pair(optionsJson, sessionId)
    }

    /**
     * Step 2: Use Android Credentials API to prompt the user and compute the signature (assertion).
     *
     * The full options JSON (including any extra server fields like `session_id`)
     * is passed to [GetPublicKeyCredentialOption]. Android Credential Manager
     * only reads the standard WebAuthn fields and ignores any extras.
     *
     * @param activity The current activity calling the FIDO2 prompt
     * @param optionsJson FIDO2 options provided by the backend
     * @return Biometric credential authentication response JSON
     */
    private suspend fun getCredentialWithFido2(
        activity: Activity,
        optionsJson: String
    ): String {
        Log.d(TAG, "Getting FIDO2 credential")
        Log.d(TAG, "=== OPTIONS JSON TO CREDENTIAL MANAGER ===")
        Log.d(TAG, optionsJson)
        Log.d(TAG, "==========================================")

        val getRequest = GetPublicKeyCredentialOption(optionsJson, null)
        val credentialRequest = GetCredentialRequest(listOf(getRequest))

        val result = getCredential(activity, credentialRequest)

        if (result.credential !is PublicKeyCredential) {
            throw Exception("Unexpected credential type")
        }

        val credential = result.credential as PublicKeyCredential
        return credential.authenticationResponseJson
    }

    /**
     * Step 3: Send the generated credential assertion JSON to the backend for cryptographic validation.
     *
     * In discoverable mode [sessionId] is non-null and is sent instead of the
     * email so the server can retrieve the matching challenge.
     *
     * @param finishUrl Endpoint URL for submitting the signature
     * @param username User email (unused in discoverable mode)
     * @param credentialJson Completed credential JSON from Android Credentials API
     * @param context Android context
     * @param sessionId UUID from [getAuthenticationOptions], null in email mode
     */
    private suspend fun verifyAuthentication(
        finishUrl: String,
        username: String,
        credentialJson: String,
        context: Context,
        sessionId: String? = null
    ) {
        Log.d(TAG, "Verifying authentication with server (discoverable=${sessionId != null})")
        val verification = fido2AuthenticateFinish(finishUrl, username, credentialJson, context, sessionId)
        Log.d(TAG, "Authentication verified: $verification")
    }
    
    companion object {
        private const val TAG = "FIDO2Service"
        const val success_message = "FIDO2 Validation Successful"
        const val cancelled_message = "FIDO2 Validation Cancelled"
    }
}
