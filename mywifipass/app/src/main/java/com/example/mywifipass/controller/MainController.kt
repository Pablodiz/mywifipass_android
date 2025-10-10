package app.mywifipass.controller

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

import app.mywifipass.model.data.Network
import app.mywifipass.model.repository.NetworkRepository
import app.mywifipass.backend.certificates.EapTLSCertificate
import app.mywifipass.backend.wifi_connection.EapTLSConnection
import app.mywifipass.backend.api_petitions.makePetitionAndAddToDatabase
import app.mywifipass.backend.api_petitions.ApiResult

import app.mywifipass.backend.database.DataSource
import app.mywifipass.R

/**
 * MainController handles the main application business logic
 * Coordinates network operations, WiFi connections, certificate management, and CSR operations
 */
class MainController(private val context: Context) {
    
    private val networkRepository = NetworkRepository(context)
    private val dataSource = app.mywifipass.backend.database.DataSource(context)
    
    /**
     * Retrieves all networks from the database
     * @return Result containing list of networks or error
     */
    suspend fun getNetworks(): Result<List<Network>> {
        return try {
            val networks = networkRepository.getNetworksFromDatabase()
            Log.d("MainController", "Retrieved ${networks.size} networks from database")
            Result.success(networks)
        } catch (e: Exception) {
            Log.e("MainController", "Error getting networks: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_get_networks) + ": ${e.message}"))
        }
    }
    
    /**
    * Adds a network from QR code scanning
    * @param qrCode QR code string containing network validation URL
    * @return Result containing the added Network or error
    */
    suspend fun addNetworkFromQR(qrCode: String, wifiManager: WifiManager): Result<Network> {
        return try {
            val networkResult = networkRepository.addNetworkFromQR(qrCode)
            if (networkResult.isFailure) {
                return networkResult
            }
            
            val network = networkResult.getOrThrow()
            try {
                checkAuthorizedAndConnect(network, wifiManager)
            } catch (e: Exception) {
                Log.w("MainController", "Failed to check authorization and connect: ${e.message}")
            }
            
            Result.success(network)
        } catch (e: Exception) {
            Log.e("MainController", "Error adding network from QR: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_add_network_from_qr) + ": ${e.message}"))
        }
    }

    /**
    * Adds a network from URL (direct input)
    * @param url Validation URL for the network
    * @return Result containing the added Network or error
    */
    suspend fun addNetworkFromUrl(url: String, wifiManager: WifiManager): Result<Network> {
        return try {
            val networkResult = networkRepository.addNetworkFromUrl(url)
            if (networkResult.isFailure) {
                return networkResult
            }
            
            val network = networkResult.getOrThrow()
            try {
                checkAuthorizedAndConnect(network, wifiManager)
            } catch (e: Exception) {
                Log.w("MainController", "Failed to check authorization and connect: ${e.message}")
            }
            
            Result.success(network)
        } catch (e: Exception) {
            Log.e("MainController", "Error adding network from URL: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_add_network_from_url) + ": ${e.message}"))
        }
    }

    
    /**
     * Connects to a WiFi network using EAP-TLS configuration
     * @param network Network to connect to
     * @param wifiManager Android WifiManager instance
     * @return Result containing success message or error
     */
    suspend fun connectToNetwork(network: Network, wifiManager: WifiManager): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MainController", "Attempting to connect to network: ${network.ssid}")
                
                // Validate network has certificates
                if (!network.are_certificiates_decrypted) {
                    return@withContext Result.failure(Exception(context.getString(R.string.network_certificates_are_not_decrypted)))
                }
                
                if (network.ca_certificate.isEmpty() || network.certificate.isEmpty() || network.private_key.isEmpty()) {
                    return@withContext Result.failure(Exception(context.getString(R.string.network_is_missing_required_certificates)))
                }
                
                // Validate certificates format
                if (!isValidCertificateFormat(network)) {
                    return@withContext Result.failure(Exception(context.getString(R.string.invalid_certificate_format)))
                }
                
                // Create EAP-TLS connection configuration
                val eapTLSConnection = createEapTLSConnection(network)
                Log.d("MainController", "Created EapTLSConnection for connection with SSID: ${network.ssid}")
                if (eapTLSConnection == null) {
                    return@withContext Result.failure(Exception(context.getString(R.string.failed_to_create_eap_tls_connection_configuration)))
                }
                
                // Attempt connection
                eapTLSConnection.connect(wifiManager, context)
                
                // Update network status
                val updatedNetwork = network.copy(is_connection_configured = true, is_user_authorized = true)
                val updateResult = networkRepository.updateNetwork(updatedNetwork)
                
                if (updateResult.isFailure) {
                    Log.w("MainController", "Failed to update network status after connection")
                }
                
                Log.d("MainController", "Successfully configured connection to: ${network.ssid}")
                Result.success(context.getString(R.string.network_connection_configured_successfully))
                
            } catch (e: Exception) {
                Log.e("MainController", "Error connecting to network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_connect_to_network) + ": ${e.message}"))
            }
        }
    }
    
    /**
     * Disconnects from a WiFi network
     * @param network Network to disconnect from
     * @param wifiManager Android WifiManager instance
     * @return Result containing success message or error
     */
    suspend fun disconnectFromNetwork(network: Network, wifiManager: WifiManager): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MainController", "Attempting to disconnect from network: ${network.ssid}")
                
                if (!network.is_connection_configured) {
                    return@withContext Result.failure(Exception(context.getString(R.string.network_is_not_configured)))
                }
                
                // Create EAP-TLS connection to get the suggestion for removal
                val eapTLSConnection = createEapTLSConnection(network)
                Log.d("MainController", "Created EapTLSConnection for disconnection with SSID: ${network.ssid}")
                if (eapTLSConnection == null) {
                    return@withContext Result.failure(Exception(context.getString(R.string.failed_to_create_eap_tls_connection_for_disconnection)))
                }
                
                // Disconnect
                eapTLSConnection.disconnect(wifiManager, context)
                
                // Update network status
                val updatedNetwork = network.copy(is_connection_configured = false)
                val updateResult = networkRepository.updateNetwork(updatedNetwork)
                
                if (updateResult.isFailure) {
                    Log.w("MainController", "Failed to update network status after disconnection")
                }
                
                Log.d("MainController", "Successfully disconnected from: ${network.ssid}")
                Result.success(context.getString(R.string.network_disconnected_successfully))
                
            } catch (e: Exception) {
                Log.e("MainController", "Error disconnecting from network: ${e.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_disconnect_from_network) + ": ${e.message}"))
            }
        }
    }
    
    /**
     * Deletes a network from the database
     * @param network Network to delete
     * @param wifiManager WifiManager instance for disconnection if needed
     * @return Result containing success message or error
     */
    suspend fun deleteNetwork(network: Network, wifiManager: WifiManager? = null): Result<String> {
        return try {
            Log.d("MainController", "Deleting network: ${network.network_common_name}")
            
            // Disconnect if currently connected
            if (network.is_connection_configured && wifiManager != null) {
                val disconnectResult = disconnectFromNetwork(network, wifiManager)
                if (disconnectResult.isFailure) {
                    Log.w("MainController", "Failed to disconnect before deletion: ${disconnectResult.exceptionOrNull()?.message}")
                }
            }
            
            // Delete from database
            val result = networkRepository.deleteNetwork(network)
            
            if (result.isSuccess) {
                Log.d("MainController", "Successfully deleted network: ${network.network_common_name}")
                Result.success(context.getString(R.string.network_deleted_successfully))
            } else {
                result.map { context.getString(R.string.network_deleted_successfully) }
            }
            
        } catch (e: Exception) {
            Log.e("MainController", "Error deleting network: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_delete_network) + ": ${e.message}"))
        }
    }
    
    /**
     * Updates a network in the database
     * @param network Network to update
     * @return Result containing success message or error
     */
    suspend fun updateNetwork(network: Network): Result<String> {
        return try {
            Log.d("MainController", "Updating network: ${network.network_common_name}")
            
            val result = networkRepository.updateNetwork(network)
            
            if (result.isSuccess) {
                Log.d("MainController", "Successfully updated network: ${network.network_common_name}")
                Result.success(context.getString(R.string.network_updated_successfully))
            } else {
                result.map { context.getString(R.string.network_updated_successfully) }
            }
            
        } catch (e: Exception) {
            Log.e("MainController", "Error updating network: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_update_network) + ": ${e.message}"))
        }
    }
    
    // /**
    //  * Gets the certificate symmetric key for a network
    //  * @param network Network to get key for
    //  * @return Result containing the symmetric key or error
    //  */
    // suspend fun getCertificatesSymmetricKey(network: Network): Result<String> {
    //     return try {
    //         Log.d("MainController", "Getting certificate symmetric key for: ${network.network_common_name}")
    //         networkRepository.getCertificatesSymmetricKey(network)
    //     } catch (e: Exception) {
    //         Log.e("MainController", "Error getting certificate symmetric key: ${e.message}")
    //         Result.failure(Exception(context.getString(R.string.failed_to_get_certificate_symmetric_key) + ": ${e.message}"))
    //     }
    // }
    
    /**
     * Validates if a network can be connected to
     * @param network Network to validate
     * @return Result containing validation message or error
     */
    fun validateNetworkForConnection(network: Network): Result<String> {
        return try {
            when {
                !network.are_certificiates_decrypted -> 
                    Result.failure(Exception(context.getString(R.string.certificates_are_not_decrypted)))
                network.ca_certificate.isEmpty() -> 
                    Result.failure(Exception(context.getString(R.string.missing_ca_certificate)))
                network.certificate.isEmpty() -> 
                    Result.failure(Exception(context.getString(R.string.missing_client_certificate)))
                network.private_key.isEmpty() -> 
                    Result.failure(Exception(context.getString(R.string.missing_private_key)))
                network.ssid.isEmpty() -> 
                    Result.failure(Exception(context.getString(R.string.missing_ssid)))
                network.network_common_name.isEmpty() -> 
                    Result.failure(Exception(context.getString(R.string.missing_network_common_name)))
                !isValidCertificateFormat(network) -> 
                    Result.failure(Exception(context.getString(R.string.invalid_certificate_format)))
                else -> 
                    Result.success(context.getString(R.string.network_is_ready_for_connection))
            }
        } catch (e: Exception) {
            Log.e("MainController", "Error validating network: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_validate_network) + ": ${e.message}"))
        }
    }
    
    // Private helper methods
    
    /**
     * Creates an EapTLSConnection instance for the given network
     * @param network Network to create connection for
     * @return EapTLSConnection instance or null if creation fails
     */
    private fun createEapTLSConnection(network: Network): EapTLSConnection? {
        return try {
            // Create EapTLSCertificate from network data
            val eapTLSCertificate = EapTLSCertificate(
                caInputStream = ByteArrayInputStream(network.ca_certificate.toByteArray()),
                clientCertInputStream = ByteArrayInputStream(network.certificate.toByteArray()),
                clientKeyInputStream = ByteArrayInputStream(network.private_key.toByteArray())
            )
            
            // Generate identity from certificate serial number
            val identity = eapTLSCertificate.clientCertificate.serialNumber.toString()
            
            // Create EapTLSConnection
            EapTLSConnection(
                ssid = network.ssid,
                eapTLSCertificate = eapTLSCertificate,
                identity = identity,
                altSubjectMatch = network.network_common_name
            )
            
        } catch (e: Exception) {
            Log.e("MainController", "Error creating EapTLSConnection: ${e.message}")
            null
        }
    }
    
    /**
     * Validates certificate format
     * @param network Network with certificates to validate
     * @return true if certificates are in valid PEM format
     */
    private fun isValidCertificateFormat(network: Network): Boolean {
        return try {
            val caCertValid = network.ca_certificate.startsWith("-----BEGIN CERTIFICATE-----") && 
                            network.ca_certificate.endsWith("-----END CERTIFICATE-----\n")
            
            val clientCertValid = network.certificate.startsWith("-----BEGIN CERTIFICATE-----") && 
                                network.certificate.endsWith("-----END CERTIFICATE-----\n")
            
            val privateKeyValid = network.private_key.startsWith("-----BEGIN PRIVATE KEY-----") && 
                                network.private_key.endsWith("-----END PRIVATE KEY-----\n")
            
            caCertValid && clientCertValid && privateKeyValid
            
        } catch (e: Exception) {
            Log.e("MainController", "Error validating certificate format: ${e.message}")
            false
        }
    }
    // CSR-related methods

    /**
     * Generates a CSR and submits it to get signed certificates for a network
     * @param network Network to generate CSR for
     * @param commonName Common name for the certificate (usually the user identifier)
     * @return Result containing success message or error
     */
    suspend fun generateAndSubmitCSR(network: Network): Result<String> {
        return try {
            Log.d("MainController", "Generating and submitting CSR for network: ${network.network_common_name}")
            val commonName = network.user_email
            // Validate inputs
            if (commonName.isBlank()) {
                return Result.failure(Exception(context.getString(R.string.common_name_cannot_be_empty)))
            }
            
            if (network.certificates_url.isEmpty()) {
                return Result.failure(Exception(context.getString(R.string.network_has_no_certificate_download_url)))
            }
            
            val result = networkRepository.generateAndSubmitCSR(network, commonName)
            
            if (result.isSuccess) {
                Log.d("MainController", "Successfully generated CSR and obtained certificates for: ${network.network_common_name}")
            }
            
            result
        } catch (e: Exception) {
            Log.e("MainController", "Error generating and submitting CSR: ${e.message}")
            Result.failure(Exception(context.getString(R.string.failed_to_generate_and_submit_csr) + ": ${e.message}"))
        }
    }

    suspend fun checkAuthorizedAndSendCSR(network: Network): Result<String> {
        return try {
            Log.d("MainController", "Checking if user is authorized for network: ${network.network_common_name}")
            
            // Use the function from the NetworkRepository
            val result = networkRepository.checkAuthorizedAndSendCSR(network)
            
            if (result.isSuccess) {
                Log.d("MainController", "Successfully checked if user is authorized for: ${network.network_common_name}")
                Result.success(context.getString(R.string.network_is_ready_for_connection))
            } else {
                result.map { context.getString(R.string.network_is_ready_for_connection) }
            }
            
        } catch (e: Exception) {
            Log.e("MainController", "Error checking if user is authorized: ${e.message}")
            Result.failure(Exception("${e.message}"))
        }
    }

    suspend fun checkAuthorizedAndConnect(network: Network, wifiManager: WifiManager): Result<String> {
        return try{
            // Check if the wifi pass is already authorized, if so send CSR
            val csrResult = checkAuthorizedAndSendCSR(network)
            
            if (csrResult.isFailure) {
                Log.d("MainController", "Failed to check if user is authorized: ${csrResult.exceptionOrNull()?.message}")
                return Result.failure(Exception(csrResult.exceptionOrNull()?.message ?: context.getString(R.string.failed_to_validate_network)))
            }
            
            Log.d("MainController", "CSR completed successfully, retrieving updated network from database")
            
            // Get the updated network from database (should now have certificates)
            val updatedNetworks = networkRepository.getNetworksFromDatabase()
            val updatedNetwork = updatedNetworks.find { it.id == network.id } ?: network
            
            Log.d("MainController", "Retrieved updated network. Certificates decrypted: ${updatedNetwork.are_certificiates_decrypted}")
            
            // Now connect with the updated network that should have certificates
            val result = connectToNetwork(updatedNetwork, wifiManager)
            if (result.isSuccess) {
                Log.d("MainController", "Successfully configured connection to network: ${updatedNetwork.network_common_name}")
                Result.success(context.getString(R.string.network_connection_configured_successfully))
            } else {
                Log.d("MainController", "Failed to connect to network: ${result.exceptionOrNull()?.message}")
                Result.failure(Exception(context.getString(R.string.failed_to_connect_to_network)))
            }
        } catch (e: Exception) {
            Log.e("MainController", "Error checking if user is authorized: ${e.message}")
            Result.failure(Exception("${e.message}"))
        }
    }
    /**
     * Adds a network from URL with full ApiResult support
     * @param url Network validation URL
     * @return ApiResult with detailed error information or success
     */
    suspend fun addNetworkFromUrlWithApiResult(url: String, wifiManager: WifiManager): ApiResult {
        return try {
            Log.d("MainController", "Adding network from URL with ApiResult: $url")
            val result = networkRepository.addNetworkFromUrlWithApiResult(url)
            
            if (result.isSuccess) {
                // Get the added network from database to attempt auto-connection
                val networks = networkRepository.getNetworksFromDatabase()
                val addedNetwork = networks.lastOrNull() // Assuming the last added network is what we want
                
                if (addedNetwork != null) {
                    try {
                        checkAuthorizedAndConnect(addedNetwork, wifiManager)
                        Log.d("MainController", "Successfully added network and attempted auto-connection")
                    } catch (e: Exception) {
                        Log.w("MainController", "Network added but failed to auto-connect: ${e.message}")
                        // Don't fail the whole operation if connection fails
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e("MainController", "Error in addNetworkFromUrlWithApiResult: ${e.message}")
            ApiResult(
                title = context.getString(R.string.network_error_title),
                message = e.message ?: context.getString(R.string.error_adding_network),
                isSuccess = false,
                showTrace = true,
                fullTrace = "MainController Exception: ${e.javaClass.simpleName}\nMessage: ${e.message}\nStackTrace: ${e.stackTraceToString()}"
            )
        }
    }

    /**
     * Adds a network from QR code scanning with full ApiResult support
     * @param qrCode QR code string containing network validation URL
     * @return ApiResult with detailed error information or success
     */
    suspend fun addNetworkFromQRWithApiResult(qrCode: String, wifiManager: WifiManager): ApiResult {
        return try {
            Log.d("MainController", "Adding network from QR with ApiResult: $qrCode")
            val result = networkRepository.addNetworkFromQRWithApiResult(qrCode)
            
            if (result.isSuccess) {
                // Get the added network from database to attempt auto-connection
                val networks = networkRepository.getNetworksFromDatabase()
                val addedNetwork = networks.lastOrNull() // Assuming the last added network is what we want
                
                if (addedNetwork != null) {
                    try {
                        checkAuthorizedAndConnect(addedNetwork, wifiManager)
                        Log.d("MainController", "Successfully added network and attempted auto-connection")
                    } catch (e: Exception) {
                        Log.w("MainController", "Network added but failed to auto-connect: ${e.message}")
                        // Don't fail the whole operation if connection fails
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e("MainController", "Error in addNetworkFromQRWithApiResult: ${e.message}")
            ApiResult(
                title = context.getString(R.string.network_error_title),
                message = e.message ?: context.getString(R.string.error_adding_network),
                isSuccess = false,
                showTrace = true,
                fullTrace = "MainController Exception: ${e.javaClass.simpleName}\nMessage: ${e.message}\nStackTrace: ${e.stackTraceToString()}"
            )
        }
    }
}