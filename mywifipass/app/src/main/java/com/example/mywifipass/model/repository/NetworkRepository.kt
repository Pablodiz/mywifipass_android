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
import app.mywifipass.backend.certificates.hexToSecretKey
import app.mywifipass.backend.certificates.decryptAES256
import app.mywifipass.backend.certificates.checkCertificates
import java.util.Enumeration
import java.security.cert.X509Certificate
import java.security.PrivateKey
import java.util.Base64
import java.io.ByteArrayOutputStream

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
                val parseResult = parseQRNetworkData(qrCode)
                if (parseResult.isFailure) {
                    return@withContext Result.failure(parseResult.exceptionOrNull()!!)
                }
                
                val qrData = parseResult.getOrNull()!!
                val validationUrl = qrData.validation_url
                
                // Now use the URL to add the network
                addNetworkFromUrl(validationUrl)
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error processing QR code: ${e.message}")
                Result.failure(Exception("Error processing QR code: ${e.message}"))
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
                var resultNetwork: Network? = null
                var errorMessage: String? = null
                
                // Use the existing makePetitionAndAddToDatabase function
                makePetitionAndAddToDatabase(
                    enteredText = url,
                    dataSource = dataSource,
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
                resultNetwork = networks.lastOrNull()
                
                if (resultNetwork != null) {
                    Result.success(resultNetwork)
                } else {
                    Result.failure(Exception("Failed to retrieve added network"))
                }
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error adding network from URL: ${e.message}")
                Result.failure(Exception("Error adding network: ${e.message}"))
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
                    return@withContext Result.failure(Exception("No certificates symmetric key available"))
                }
                
                var errorMessage: String? = null
                var keyStore: KeyStore? = null
                
                // Step 2: Download certificates using existing function
                getCertificates(
                    endpoint = network.certificates_url,
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
                    val extractResult = extractCertificatesFromKeyStore(ks, network.certificates_symmetric_key)
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
                    
                    Result.success("Certificates downloaded and decrypted successfully")
                } ?: Result.failure(Exception("Failed to download certificates"))
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error downloading certificates: ${e.message}")
                Result.failure(Exception("Certificate download failed: ${e.message}"))
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
                Result.failure(Exception("Failed to delete network: ${e.message}"))
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
                Result.failure(Exception("Failed to update network: ${e.message}"))
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
                Result.failure(Exception("Failed to insert network: ${e.message}"))
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
                    return@withContext Result.failure(Exception("No validation URL available"))
                }
                
                var symmetricKey: String? = null
                var errorMessage: String? = null
                
                getCertificatesSymmetricKey(
                    endpoint = network.validation_url,
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
                } ?: Result.failure(Exception("Failed to retrieve symmetric key"))
                
            } catch (e: Exception) {
                Log.e("NetworkRepository", "Error getting certificates symmetric key: ${e.message}")
                Result.failure(Exception("Failed to get symmetric key: ${e.message}"))
            }
        }
    }
    
    // Private helper methods
    
    /**
    * Parses QR code string to extract network validation data
    * @param qrCode QR code string (just a URL)
    * @return Result containing QrData or error
    */
    private fun parseQRNetworkData(qrCode: String): Result<QrData> {
        return try {
            val url = qrCode.trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Result.success(QrData(validation_url = url))
            } else {
                Result.failure(Exception("QR code does not contain a valid URL"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error parsing QR code: ${e.message}"))
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
     * @return Result containing extracted certificates or error
     */
    private fun extractCertificatesFromKeyStore(keyStore: KeyStore, password: String): Result<ExtractedCertificates> {
        return try {
            var caCertificate = ""
            var clientCertificate = ""
            var privateKey = ""
            
            // Iterate through KeyStore aliases
            val aliases: Enumeration<String> = keyStore.aliases()
            
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                
                // Get certificate
                val certificate = keyStore.getCertificate(alias)
                if (certificate is X509Certificate) {
                    val certPem = convertCertificateToPem(certificate)
                    
                    // Determine if it's a CA certificate or client certificate
                    // CA certificates usually have CA:TRUE in basic constraints
                    val basicConstraints = certificate.basicConstraints
                    if (basicConstraints >= 0) {
                        // This is a CA certificate
                        caCertificate = certPem
                    } else {
                        // This is a client certificate
                        clientCertificate = certPem
                    }
                }
                
                // Get private key
                val key = keyStore.getKey(alias, password.toCharArray())
                if (key is PrivateKey) {
                    privateKey = convertPrivateKeyToPem(key)
                }
            }
            
            // Validate that we have all required certificates
            if (checkCertificates(caCertificate, clientCertificate, privateKey)) {
                Result.success(ExtractedCertificates(caCertificate, clientCertificate, privateKey))
            } else {
                Result.failure(Exception("Invalid certificate format or missing certificates"))
            }
            
        } catch (e: Exception) {
            Log.e("NetworkRepository", "Error extracting certificates: ${e.message}")
            Result.failure(Exception("Failed to extract certificates: ${e.message}"))
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