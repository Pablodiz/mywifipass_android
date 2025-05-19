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
        throw Exception("Unrecognized response format")
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
            val reply = httpPetition(enteredText)
            val body = reply.body
            val statusCode = reply.statusCode
            when (statusCode) {
                200 -> {
                    val network = parseReply(body)
                    dataSource.insertNetwork(network)
                    onSuccess(body) 
                }
                400 -> {
                    onError("Bad request")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                404 -> {
                    onError("No user found")
                }
                500 -> {
                    onError("Server or petition error")
                }
                else -> {
                    onError("An unexpected error occurred: $body")
                }
            }

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

@Serializable
data class CheckAttendeeResponse(
    val id_document: String,
    val name: String,
    val authorize_url: String
)

suspend fun authorizeAttendee(
    endpoint: String,
    token: String,
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint, jsonString="", token = token)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess("Attendee authorized successfully")
                }
                400 -> {
                    onError("Bad request: $body")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                else -> {
                    onError("An unexpected error occurred: $body")
                }
            }
        } catch (e: Exception) {
            onError("An error occurred: ${e.message}")
        }
    }
}
suspend fun checkAttendee(
    endpoint: String,
    body: String, 
    token: String,
    onError: (String) -> Unit = {},
    onSuccess: (String, String) -> Unit = {_,_ ->}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint, jsonString = body,  token = token)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    try{
                        val constructor_json = Json { ignoreUnknownKeys = true }
                        val parsed_reply = constructor_json.decodeFromString<CheckAttendeeResponse>(body)
                        onSuccess("""
                            Name: ${parsed_reply.name}
                            ID Document: ${parsed_reply.id_document}
                        """.trimIndent(), parsed_reply.authorize_url
                        )

                    } catch (e: Exception){
                        onError("Error parsing the response: ${e.message}")
                        return@withContext
                    }
                }
                400 -> {
                    onError("Bad request: $body")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                403 -> {
                    onError("Access denied for the attendee")
                }
                404 -> {
                    onError("No attendee found with the provided data")
                }
                else -> {
                    onError("An unexpected error occurred: $body")
                }
            }
        } catch (e: Exception) {
            onError("An error occurred: ${e.message}")
        }
    }
}

fun buildUrl(baseUrl: String, endpoint: String): String {
    return baseUrl.trimEnd('/') + "/" + endpoint.trimStart('/')
}


suspend fun loginPetition(
    url: String,
    login: String,
    pwd: String, 
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}, 
    usePassword: Boolean = true
) {
    withContext(Dispatchers.IO) {
        try {
            var endpoint : String
            var credentials : Map<String, String>
            if (usePassword){
                endpoint = buildUrl(url, "/api/login/password")
                credentials = mapOf("username" to login, "password" to pwd)

            }else {
                endpoint = url
                credentials = mapOf("username" to login, "token" to pwd)
            }
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