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
import app.mywifipass.backend.api_petitions.getCertificates
import app.mywifipass.backend.api_petitions.CertificatesResponse
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.util.Base64
import app.mywifipass.R

import app.mywifipass.backend.extractURLFromParameter

/**
 * Repository for handling network data operations
 * Implements database operations, certificate downloads, and QR network parsing
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
    * Adds a network from a URL (either from QR or direct input)
    * @param url Validation URL for the network
    * @return Result containing the added Network or error
    */
    suspend fun addNetworkFromUrl(url: String): Result<Network> {
        return withContext(Dispatchers.IO) {
            try {
                var errorMessage: String? = null
                
                // Use the existing makePetitionAndAddToDatabase function
                makePetitionAndAddToDatabase(
                    enteredText = url,
                    dataSource = dataSource,
                    context = context,
                    onSuccess = { body ->
                        Log.d("NetworkRepository", "Network added successfully: $body")
                    },
                    onError = { error ->
                        errorMessage = error
                        Log.e("NetworkRepository", "Error adding network: $error")
                    }
                )
                
                // Check if there was an error
                errorMessage?.let {
                    return@withContext Result.failure(Exception(it))
                }
                
                // Get the latest network (should be the one we just added)
                val networks = dataSource.loadConnections()
                var resultNetwork = networks.lastOrNull()
                
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
     * Downloads and decrypts certificates for a network
     * @param network Network to download certificates for
     * @return Result containing success message or error
     */
    suspend fun downloadCertificates(network: Network): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Get certificates symmetric key if not already present
                if (network.certificates_symmetric_key.isEmpty()) {
                    return@withContext Result.failure(Exception(context.getString(R.string.no_certificates_symmetric_key_available)))
                }
                
                var errorMessage: String? = null
                var keyStore: KeyStore? = null
                
                // Step 2: Download certificates using existing function
                getCertificates(
                    endpoint = network.certificates_url,
                    context = context,
                    key = network.certificates_symmetric_key,
                    onSuccess = { downloadedKeyStore ->
                        keyStore = downloadedKeyStore
                        Log.d("NetworkRepository", "Certificates downloaded successfully")
                    },
                    onError = { error ->
                        errorMessage = error
                        Log.e("NetworkRepository", "Error downloading certificates: $error")
                    }
                )
                
                // Check for download errors
                errorMessage?.let {
                    return@withContext Result.failure(Exception(it))
                }
                
                keyStore?.let { ks ->
                    // Step 3: Extract certificates from KeyStore
                    val extractResult = extractCertificatesFromKeyStore(ks, network.certificates_symmetric_key, context)
                    if (extractResult.isFailure) {
                        return@withContext Result.failure(extractResult.exceptionOrNull()!!)
                    }
                    
                    val certificates = extractResult.getOrNull()!!
                    
                    // Step 4: Update network with certificates
                    val updatedNetwork = network.copy(
                        ca_certificate = certificates.caCertificate,
                        certificate = certificates.clientCertificate,
                        private_key = certificates.privateKey,
                        is_certificates_key_set = true,
                        are_certificiates_decrypted = true
                    )
                    
                    // Step 5: Update in database
                    dataSource.updateNetwork(updatedNetwork)
                    
                    Result.success(context.getString(R.string.certificates_downloaded_and_decrypted_successfully))
                } ?: Result.failure(Exception(context.getString(R.string.failed_to_download_certificates)))
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error downloading certificates: ${e.message}")
                Result.failure(Exception(context.getString(R.string.certificate_download_failed) + ": ${e.message}"))
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
     * Gets certificates symmetric key for a network
     * @param network Network to get key for
     * @return Result containing the symmetric key or error
     */
    suspend fun getCertificatesSymmetricKey(network: Network): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (network.validation_url.isEmpty()) {
                    return@withContext Result.failure(Exception(context.getString(R.string.no_validation_url_available)))
                }
                
                var symmetricKey: String? = null
                var errorMessage: String? = null
                
                getCertificatesSymmetricKey(
                    endpoint = network.validation_url,
                    context = context,
                    onSuccess = { key ->
                        symmetricKey = key
                        Log.d("NetworkRepository", "Symmetric key retrieved successfully")
                    },
                    onError = { error ->
                        errorMessage = error
                        Log.e("NetworkRepository", "Error getting symmetric key: $error")
                    }
                )
                
                errorMessage?.let {
                    return@withContext Result.failure(Exception(it))
                }
                
                symmetricKey?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(context.getString(R.string.failed_to_get_symmetric_key)))
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error getting certificates symmetric key: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_get_symmetric_key) + ": ${e.message}"))
            }
        }
    }
    
    // Private helper methods
    
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
     * Data class to hold extracted certificates
     */
    private data class ExtractedCertificates(
        val caCertificate: String,
        val clientCertificate: String,
        val privateKey: String
    )
    
    /**
     * Extracts certificates from KeyStore and converts them to PEM format
     * @param keyStore KeyStore containing certificates
     * @param password Password for the KeyStore
     * @param context Android context for string resources
     * @return Result containing extracted certificates or error
     */
    private fun extractCertificatesFromKeyStore(keyStore: KeyStore, password: String, context: Context): Result<ExtractedCertificates> {
        return try {
            val aliases = keyStore.aliases()
            var foundAlias: String? = null
            var userCert: Certificate? = null
            var caCert: Certificate? = null
            var privateKey: PrivateKey? = null
            var ca: Array<Certificate>? = null
            // Find all certificates found in the store
            var contador: Int = 0
            while (aliases.hasMoreElements()) {
                val a = aliases.nextElement()
                // Obtain private key 
                val pk = keyStore.getKey(a, password.toCharArray())
                // Obtain certificate chain
                val chain = keyStore.getCertificateChain(a)
                // Get user and CA certs from the cert chain
                if (pk != null && chain != null && chain.size >= 2) {
                    foundAlias = a
                    userCert = chain[0]
                    privateKey = pk as? PrivateKey
                    caCert = chain[1]
                    break
                }
            }

            // Encode in PEM format
            val certPem = convertCertificateToPem(userCert as X509Certificate)
            val caPem  = convertCertificateToPem(caCert as X509Certificate)
            val privateKeyPem = convertPrivateKeyToPem(privateKey as PrivateKey)

            if (checkCertificates(caPem, certPem, privateKeyPem)) {
                Result.success(ExtractedCertificates(caPem, certPem, privateKeyPem))
            } else {
                Result.failure(Exception(context.getString(R.string.could_not_find_valid_certificate_chain)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(context.getString(R.string.failed_to_extract_certificates) + ": ${e.message}"))
        }
    }
    
    /**
     * Converts X509Certificate to PEM format
     */
    private fun convertCertificateToPem(certificate: X509Certificate): String {
        val encoded = Base64.getEncoder().encode(certificate.encoded)
        val pemCert = StringBuilder()
        pemCert.append("-----BEGIN CERTIFICATE-----\n")
        
        // Split the base64 string into 64-character lines
        val certString = String(encoded)
        var i = 0
        while (i < certString.length) {
            val end = minOf(i + 64, certString.length)
            pemCert.append(certString.substring(i, end)).append("\n")
            i += 64
        }
        
        pemCert.append("-----END CERTIFICATE-----\n")
        return pemCert.toString()
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
}