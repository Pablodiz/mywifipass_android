package com.example.get_eap_tls.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate

import com.example.get_eap_tls.backend.database.Network
import com.example.get_eap_tls.backend.database.DataSource
import com.example.get_eap_tls.backend.httpPetition
import com.example.get_eap_tls.backend.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

fun parseReply(string: String): Network{
    return try{
        val constructor_json = Json { ignoreUnknownKeys = true }
        constructor_json.decodeFromString<Network>(string)
    } catch (e: Exception){
        throw Exception("Error al procesar la respuesta: ${e.message}")
    }
}

suspend fun makePetitionAndAddToDatabase(
    enteredText: String,
    dataSource: DataSource, 
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
):List<Network> {
    withContext(Dispatchers.IO) {
        try{
            val reply = httpPetition(enteredText).body
            val network = parseReply(reply)
            dataSource.insertNetwork(network)
            onSuccess(reply) 
        } catch (e:Exception){
            onError(e.message.toString())
        }
    }
    return dataSource.loadConnections()
}

@Serializable
data class CertificatesSymmetricKey(
    val certificates_symmetric_key: String
)

suspend fun getCertificatesSymmetricKey(
    endpoint: String, 
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(endpoint)
            val statusCode = httpResponse.statusCode
            val reply = httpResponse.body
            when (statusCode) {
                200 -> {
                    val constructor_json = Json { ignoreUnknownKeys = true }
                    val symmetricKeyJSON = constructor_json.decodeFromString<CertificatesSymmetricKey>(reply)
                    onSuccess(symmetricKeyJSON.certificates_symmetric_key)
                }
                403 -> {
                    onError("Access denied")
                }
                else -> {
                    onError("An unexpected error occurred: $statusCode")
                }
            }
        } catch (e: Exception) {
            onError(e.message.toString())
        }
    }
}

suspend fun allowAccess(
    endpoint: String,
    body: String, 
    token: String,
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint, jsonString = body,  token = token)
            val statusCode = httpResponse.statusCode
            val reply = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess("The attendee has been granted access")
                }
                400 -> {
                    onError("Bad request: $reply")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                403 -> {
                    onError("Access denied for the attendee")
                }
                404 -> {
                    onError("Attendee not found")
                }
                else -> {
                    onError("An unexpected error occurred: $reply")
                }
            }
        } catch (e: Exception) {
            onError("An error occurred: ${e.message}")
        }
    }
}


suspend fun loginPetition(
    url: String,
    login: String,
    pwd: String, 
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val endpoint = "$url/api/api-token-auth/"
            val credentials = mapOf("username" to login, "password" to pwd)
            val constructorJson = Json { ignoreUnknownKeys = true }
            val jsonString = constructorJson.encodeToString(credentials)
            val response = httpPetition(endpoint, jsonString)
            val statusCode = response.statusCode
            val responseBody = response.body

            when (statusCode) {
                200 -> {
                    // Get the token 
                    val jsonResponse = constructorJson.decodeFromString<Map<String, String>>(responseBody)
                    val token = jsonResponse["token"] ?: throw Exception("Token not found in response")
                    onSuccess(token)
                }
                400 -> {
                    onError("Incorrect login credentials")
                }
                else -> {
                    onError("An unexpected error occurred: $statusCode")
                }
            }
        } catch (e: Exception) {
            onError("An error occurred: ${e.message}")
        }
    }
}