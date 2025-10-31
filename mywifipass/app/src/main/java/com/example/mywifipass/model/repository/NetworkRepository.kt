package app.mywifipass.model.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore

import app.mywifipass.model.data.Network
import app.mywifipass.model.data.QrData
import app.mywifipass.backend.database.DataSource
import app.mywifipass.backend.api_petitions.*
import app.mywifipass.backend.certificates.checkCertificates
import java.util.Enumeration
import java.security.cert.X509Certificate
import java.security.cert.Certificate
import java.security.PrivateKey
import java.io.ByteArrayOutputStream

// Imports for setting the certificates
// import app.mywifipass.backend.api_petitions.getCertificates
// import app.mywifipass.backend.api_petitions.CertificatesResponse
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.util.Base64
import app.mywifipass.R

import app.mywifipass.backend.extractURLFromParameter

// Imports for CSR generation and submission
import app.mywifipass.backend.certificates.generateKeyPair
import app.mywifipass.backend.certificates.generateCSR
import app.mywifipass.backend.certificates.csrToPem
import app.mywifipass.backend.api_petitions.sendCSR
import app.mywifipass.backend.api_petitions.checkUserAuthorized
import app.mywifipass.backend.api_petitions.CSRResponse
import java.security.KeyPair

import kotlinx.coroutines.launch
/**
 * Repository for handling network data operations
 * Implements database operations, certificate downloads, QR network parsing, and CSR operations
 */
class NetworkRepository(private val context: Context) {
    
    private val dataSource = DataSource(context)
    
    /**
     * Retrieves all networks from the local database
     * @return List of networks from database
     */
    suspend fun getNetworksFromDatabase(): List<Network> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.loadConnections()
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error loading networks from database: ${e.message}")
                emptyList()
            }
        }
    }
    
   /**
    * Adds a network from QR code scanning
    * @param qrCode QR code string containing network validation URL
    * @return Result containing the added Network or error
    */
    suspend fun addNetworkFromQR(qrCode: String): Result<Network> {
        return withContext(Dispatchers.IO) {
            try {
                // Parse QR code to extract URL
                val parseResult = parseQRNetworkData(qrCode, context)
                if (parseResult.isFailure) {
                    return@withContext Result.failure(parseResult.exceptionOrNull()!!)
                }
                
                val url = parseResult.getOrNull()!!
                
                // Now use the URL to add the network
                addNetworkFromUrl(url)
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error processing QR code: ${e.message}")
                Result.failure(Exception(context.getString(R.string.error_processing_qr_code) + ": ${e.message}"))
            }
        }
    }

    /**
    * Adds a network from QR code scanning with full ApiResult support
    * @param qrCode QR code string containing network validation URL
    * @return ApiResult with detailed error information or success
    */
    suspend fun addNetworkFromQRWithApiResult(qrCode: String): ApiResult {
        return withContext(Dispatchers.IO) {
            try {
                // Parse QR code to extract URL
                val parseResult = parseQRNetworkData(qrCode, context)
                if (parseResult.isFailure) {
                    return@withContext ApiResult(
                        title = context.getString(R.string.parsing_error_title),
                        message = parseResult.exceptionOrNull()?.message ?: context.getString(R.string.error_parsing_qr_code),
                        isSuccess = false,
                        showTrace = true,
                        fullTrace = "QR Parsing Error: ${parseResult.exceptionOrNull()?.stackTraceToString()}"
                    )
                }
                
                val url = parseResult.getOrNull()!!
                
                // Now use the URL to add the network with ApiResult
                addNetworkFromUrlWithApiResult(url)
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Exception in addNetworkFromQRWithApiResult: ${e.message}")
                ApiResult(
                    title = context.getString(R.string.network_error_title),
                    message = context.getString(R.string.error_processing_qr_code) + ": ${e.message}",
                    isSuccess = false,
                    showTrace = true,
                    fullTrace = "Exception: ${e.javaClass.simpleName}\nMessage: ${e.message}\nStackTrace: ${e.stackTraceToString()}"
                )
            }
        }
    }

    suspend fun checkAuthorizedAndSendCSR(network: Network): Result<String> {
        val user_email = network.user_email 
        return withContext(Dispatchers.IO) {
            try {
                var authorizationResult: Result<String>? = null
                
                // Check if the wifi pass is already authorized
                checkUserAuthorized(network.check_user_authorized_url, 
                    context,
                    onSuccess = { isAuthorized ->
                        if (isAuthorized){
                            // Authorization successful, we'll generate CSR outside the callback
                            authorizationResult = Result.success("User is authorized")
                        } else {
                            authorizationResult = Result.failure(Exception("User is not authorized"))
                        }
                    },
                    onError = {
                        Log.e("NetworkRepository", "Error checking user authorization: $it")
                        authorizationResult = Result.failure(Exception("Authorization check failed: $it"))
                    }
                )
                
                // Wait for authorization check to complete
                while (authorizationResult == null) {
                    kotlinx.coroutines.delay(50) // Wait 50ms before checking again
                }
                
                // If authorized, generate and submit CSR synchronously
                if (authorizationResult!!.isSuccess) {
                    Log.d("NetworkRepository", "User authorized, proceeding with CSR generation")
                    val csrResult = generateAndSubmitCSR(network, user_email)
                    return@withContext csrResult
                } else {
                    Log.d("NetworkRepository", "User not authorized: ${authorizationResult!!.exceptionOrNull()?.message}")
                    return@withContext authorizationResult!!
                }
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error validating network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_validate_network) + ": ${e.message}"))
            }
        }
    }

    /**
    * Adds a network from a URL (either from QR or direct input)
    * @param url Validation URL for the network
    * @return Result containing the added Network or error
    */
    suspend fun addNetworkFromUrl(url: String): Result<Network> {
        return withContext(Dispatchers.IO) {
            try {
                var apiError: ApiResult? = null
                var apiSuccess: ApiResult? = null
                
                // Use the existing downloadWifiPass function
                downloadWifiPass(
                    enteredText = url,
                    dataSource = dataSource,
                    context = context,
                    onSuccess = { apiResult: ApiResult ->
                        apiSuccess = apiResult
                        Log.d("NetworkRepository", "Network added successfully: ${apiResult.message}")
                    },
                    onError = { apiResult: ApiResult ->
                        apiError = apiResult
                        Log.e("NetworkRepository", "Error adding network: ${apiResult.message}")
                    }
                )
                
                // Check if there was an API error
                apiError?.let { error ->
                    return@withContext Result.failure(Exception(error.message))
                }
                
                // Get the latest network (should be the one we just added)
                val networks = dataSource.loadConnections()
                val resultNetwork = networks.lastOrNull()

                if (resultNetwork != null) {      
                    Result.success(resultNetwork)
                } else {
                    Result.failure(Exception(context.getString(R.string.failed_to_retrieve_added_network)))
                }
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error adding network from URL: ${e.message}")
                Result.failure(Exception(context.getString(R.string.error_adding_network) + ": ${e.message}"))
            }
        }
    }

    /**
    * Adds a network from a URL with full ApiResult support
    * @param url Validation URL for the network
    * @return ApiResult with detailed error information or success
    */
    suspend fun addNetworkFromUrlWithApiResult(url: String): ApiResult {
        return withContext(Dispatchers.IO) {
            try {
                var result: ApiResult? = null
                
                // Use the existing downloadWifiPass function
                downloadWifiPass(
                    enteredText = url,
                    dataSource = dataSource,
                    context = context,
                    onSuccess = { apiResult: ApiResult ->
                        result = apiResult
                        Log.d("NetworkRepository", "Network added successfully: ${apiResult.message}")
                    },
                    onError = { apiResult: ApiResult ->
                        result = apiResult
                        Log.e("NetworkRepository", "Error adding network: ${apiResult.message}")
                    }
                )
                
                // Return the ApiResult (either success or error)
                result ?: ApiResult(
                    title = context.getString(R.string.network_error_title),
                    message = context.getString(R.string.error_adding_network),
                    isSuccess = false,
                    showTrace = false,
                    fullTrace = null
                )
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Exception in addNetworkFromUrlWithApiResult: ${e.message}")
                ApiResult(
                    title = context.getString(R.string.network_error_title),
                    message = context.getString(R.string.error_adding_network) + ": ${e.message}",
                    isSuccess = false,
                    showTrace = true,
                    fullTrace = "Exception: ${e.javaClass.simpleName}\nMessage: ${e.message}\nStackTrace: ${e.stackTraceToString()}"
                )
            }
        }
    }
    
    /**
     * Deletes a network from the database
     * @param network Network to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteNetwork(network: Network): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.deleteNetwork(network)
                Log.d("NetworkRepository", "Network deleted successfully: ${network.network_common_name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error deleting network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_delete_network) + ": ${e.message}"))
            }
        }
    }
    
    /**
     * Updates a network in the database
     * @param network Network to update
     * @return Result indicating success or failure
     */
    suspend fun updateNetwork(network: Network): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.updateNetwork(network)
                Log.d("NetworkRepository", "Network updated successfully: ${network.network_common_name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error updating network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_update_network) + ": ${e.message}"))
            }
        }
    }
    
    /**
     * Inserts a new network into the database
     * @param network Network to insert
     * @return Result indicating success or failure
     */
    suspend fun insertNetwork(network: Network): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.insertNetwork(network)
                Log.d("NetworkRepository", "Network inserted successfully: ${network.network_common_name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error inserting network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_insert_network) + ": ${e.message}"))
            }
        }
    }
    
    /**
    * Parses QR code string to extract network validation data
    * @param qrCode QR code string (just a URL)
    * @param context Android context for string resources
    * @return Result containing QrData or error
    */
    private fun parseQRNetworkData(qrCode: String, context: Context): Result<String> {
        return try {
            val url = qrCode.trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                val result = extractURLFromParameter(url)
                Result.success(result)
            } else {
                Result.failure(Exception(context.getString(R.string.qr_code_invalid_url)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(context.getString(R.string.error_parsing_qr_code) + ": ${e.message}"))
        }
    }
    
    /**
     * Converts PrivateKey to PEM format
     */
    private fun convertPrivateKeyToPem(privateKey: PrivateKey): String {
        val encoded = Base64.getEncoder().encode(privateKey.encoded)
        val pemKey = StringBuilder()
        pemKey.append("-----BEGIN PRIVATE KEY-----\n")
        
        // Split the base64 string into 64-character lines
        val keyString = String(encoded)
        var i = 0
        while (i < keyString.length) {
            val end = minOf(i + 64, keyString.length)
            pemKey.append(keyString.substring(i, end)).append("\n")
            i += 64
        }
        
        pemKey.append("-----END PRIVATE KEY-----\n")
        return pemKey.toString()
    }

    /**
     * Generates a CSR for a network and sends it to get signed certificates
     * @param network Network to generate CSR for
     * @param commonName Common name for the certificate (usually the user identifier)
     * @return Result containing success message or error
     */
    suspend fun generateAndSubmitCSR(network: Network, commonName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NetworkRepository", "Generating CSR for network: ${network.network_common_name}")
                
                // Validate network has CSR endpoint
                if (network.certificates_url.isEmpty()) {
                    return@withContext Result.failure(Exception(context.getString(R.string.network_has_no_certificate_download_url)))
                }
                
                // Step 1: Generate key pair
                val keyPair = generateKeyPair()
                Log.d("NetworkRepository", "Generated key pair for CSR")
                
                // Step 2: Generate CSR
                val csr = generateCSR(keyPair, commonName)
                val csrPem = csrToPem(csr)
                Log.d("NetworkRepository", "Generated CSR in PEM format")
                
                // Step 3: Send CSR to server
                var errorMessage: String? = null
                var csrResponse: CSRResponse? = null
                
                sendCSR(
                    endpoint = network.certificates_url,
                    csrPem = csrPem,
                    token = network.certificates_symmetric_key,
                    context = context,
                    onSuccess = { response ->
                        csrResponse = response
                        Log.d("NetworkRepository", "CSR submitted successfully and certificates received")
                    },
                    onError = { apiResult ->
                        errorMessage = apiResult.message
                        Log.e("NetworkRepository", "Error submitting CSR: ${apiResult.message}")
                    }
                )
                
                // Check for submission errors
                errorMessage?.let {
                    return@withContext Result.failure(Exception(it))
                }
                
                csrResponse?.let { response ->
                    // Step 4: Validate response contains required certificates
                    if (response.signed_cert.isNullOrEmpty() || response.ca_cert.isNullOrEmpty()) {
                        return@withContext Result.failure(Exception(context.getString(R.string.invalid_csr_response_missing_certificates)))
                    }
                    
                    // Step 5: Convert private key to PEM format
                    val privateKeyPem = convertPrivateKeyToPem(keyPair.private)
                    
                    // Step 6: Validate certificates format
                    if (!isValidPemCertificate(response.signed_cert) || 
                        !isValidPemCertificate(response.ca_cert) ||
                        !isValidPemPrivateKey(privateKeyPem)) {
                        return@withContext Result.failure(Exception(context.getString(R.string.invalid_certificate_format_from_csr)))
                    }
                    
                    // Step 7: Update network with new certificates
                    val updatedNetwork = network.copy(
                        ca_certificate = response.ca_cert,
                        certificate = response.signed_cert,
                        private_key = privateKeyPem,
                        is_certificates_key_set = true,
                        are_certificiates_decrypted = true
                    )
                    
                    // Step 8: Update in database
                    val updateResult = updateNetwork(updatedNetwork)
                    if (updateResult.isFailure) {
                        return@withContext Result.failure(Exception(context.getString(R.string.failed_to_update_network_with_csr_certificates)))
                    }
                    
                    Log.d("NetworkRepository", "CSR process completed successfully for network: ${network.network_common_name}")
                    Result.success(context.getString(R.string.csr_generated_and_certificates_obtained_successfully))
                } ?: Result.failure(Exception(context.getString(R.string.failed_to_receive_csr_response)))
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error generating and submitting CSR: ${e.message}")
                Result.failure(Exception(context.getString(R.string.csr_generation_failed) + ": ${e.message}"))
            }
        }
    }
    /**
     * Validates if a string is a valid PEM certificate
     */
    private fun isValidPemCertificate(pemData: String): Boolean {
        return pemData.startsWith("-----BEGIN CERTIFICATE-----") && 
               pemData.endsWith("-----END CERTIFICATE-----\n")
    }

    /**
     * Validates if a string is a valid PEM private key
     */
    private fun isValidPemPrivateKey(pemData: String): Boolean {
        return pemData.startsWith("-----BEGIN PRIVATE KEY-----") && 
               pemData.endsWith("-----END PRIVATE KEY-----\n")
    }
}