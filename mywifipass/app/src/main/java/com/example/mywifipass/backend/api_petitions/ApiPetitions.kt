package app.mywifipass.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.mywifipass.backend.certificates.EapTLSCertificate

import app.mywifipass.model.data.Network
import app.mywifipass.backend.database.DataSource
import app.mywifipass.backend.httpPetition
import app.mywifipass.backend.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.security.KeyStore

fun parseReply(string: String): Network{
    return try{
        val constructor_json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        constructor_json.decodeFromString<Network>(string)
    } catch (e: Exception){
        throw Exception("Unrecognized response format")
    }
}

suspend fun confirmDownload(
    has_downloaded_url: String,
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(has_downloaded_url, jsonString = "")
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess(body)
                }
                400 -> {
                    onError("Bad request")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                else -> {
                    onError("An unexpected error occurred: $body")
                }
            }
        } catch (e: Exception) {
            onError(e.message.toString())
        }
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
                    confirmDownload(network.has_downloaded_url)
                    onSuccess(body) 
                }
                400 -> {
                    onError("Bad request")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                403 -> {
                    onError(extractErrorMessage(body))
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
data class CertificatesResponse(
    val pkcs12_b64: String,
)

suspend fun getCertificates(
    endpoint: String,
    onError: (String) -> Unit = {},
    onSuccess: (KeyStore) -> Unit = {},
    key: String 
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(endpoint)
            val statusCode = httpResponse.statusCode
            val reply = httpResponse.body
            when (statusCode) {
                200 -> {
                    val json = Json { ignoreUnknownKeys = true }
                    val certs = json.decodeFromString<CertificatesResponse>(reply)
                    val pkcs12Bytes = Base64.decode(certs.pkcs12_b64, Base64.DEFAULT)
                    val inputStream = ByteArrayInputStream(pkcs12Bytes)
                    val keyStore = KeyStore.getInstance("PKCS12")
                    keyStore.load(inputStream, key.toCharArray()) 
                    onSuccess(keyStore)
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

fun extractErrorMessage(body: String): String {
    return try {
        val json = Json.parseToJsonElement(body).jsonObject
        json["error"]?.jsonPrimitive?.content ?: "Unknown error"
    } catch (e: Exception) {
        "Unknown error"
    }
}

suspend fun checkAttendee(
    endpoint: String,
    token: String,
    onError: (String) -> Unit = {},
    onSuccess: (String, String) -> Unit = {_,_ ->}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint,  token = token)
            val statusCode = httpResponse.statusCode
            val response = httpResponse.body
            when (statusCode) {
                200 -> {
                    try{
                        val constructor_json = Json { ignoreUnknownKeys = true }
                        val parsed_reply = constructor_json.decodeFromString<CheckAttendeeResponse>(response)
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
                    onError("Bad request: ${extractErrorMessage(response)}")
                }
                401 -> {
                    onError("You are not authorized to access this resource")
                }
                403 -> {
                    onError(extractErrorMessage(response))
                }
                404 -> {
                    onError("No attendee found with the provided data")
                }
                else -> {
                    onError("An unexpected error occurred: ${extractErrorMessage(response)}")
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