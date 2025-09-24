package app.mywifipass.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.mywifipass.backend.certificates.EapTLSCertificate
import app.mywifipass.backend.certificates.generateKeyPair
import app.mywifipass.backend.certificates.generateCSR
import app.mywifipass.backend.certificates.csrToPem

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
import app.mywifipass.R

fun parseReply(string: String, context: Context): Network{
    return try{
        val constructor_json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        constructor_json.decodeFromString<Network>(string)
    } catch (e: Exception){
        throw Exception(context.getString(R.string.unrecognized_response_format))
    }
}

suspend fun confirmDownload(
    has_downloaded_url: String,
    context: Context,
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(has_downloaded_url, jsonString = "", context = context)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess(body)
                }
                400 -> {
                    onError(context.getString(R.string.error_400))
                }
                401 -> {
                    onError(context.getString(R.string.error_401))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $body")
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
    context: Context, 
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
):List<Network> {
    withContext(Dispatchers.IO) {
        try{
            val reply = httpPetition(enteredText, context = context)
            val body = reply.body
            val statusCode = reply.statusCode
            when (statusCode) {
                200 -> {
                    val network = parseReply(body, context)
                    dataSource.insertNetwork(network)
                    confirmDownload(network.has_downloaded_url, context)
                    onSuccess(body) 
                }
                400 -> {
                    onError(context.getString(R.string.error_400))
                }
                401 -> {
                    onError(context.getString(R.string.error_401))
                }
                403 -> {
                    onError(extractErrorMessage(body, context))
                }
                404 -> {
                    onError(context.getString(R.string.error_404))
                }
                500 -> {
                    onError(context.getString(R.string.error_500))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $body")
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
    context: Context, 
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(endpoint, context = context)
            val statusCode = httpResponse.statusCode
            val reply = httpResponse.body
            when (statusCode) {
                200 -> {
                    val constructor_json = Json { ignoreUnknownKeys = true }
                    val symmetricKeyJSON = constructor_json.decodeFromString<CertificatesSymmetricKey>(reply)
                    onSuccess(symmetricKeyJSON.certificates_symmetric_key)
                }
                403 -> {
                    onError(context.getString(R.string.error_403))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $statusCode")
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
    context: Context,
    onError: (String) -> Unit = {},
    onSuccess: (KeyStore) -> Unit = {},
    key: String 
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(endpoint, context = context)
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
                    onError(context.getString(R.string.error_403))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $statusCode")
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
    context: Context,
    onError: (String) -> Unit = {},
    onSuccess: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint, jsonString="", token = token, context = context)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess(context.getString(R.string.attendee_authorized_successfully))
                }
                400 -> {
                    onError(context.getString(R.string.error_400) + ": $body")
                }
                401 -> {
                    onError(context.getString(R.string.error_401))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": ${extractErrorMessage(body, context)}")
                }
            }
        } catch (e: Exception) {
            onError(context.getString(R.string.error) + ": ${e.message}")
        }       
    }
}

fun extractErrorMessage(body: String, context: Context): String {
    return try {
        val json = Json.parseToJsonElement(body).jsonObject
        json["error"]?.jsonPrimitive?.content ?: context.getString(R.string.unknown_error)
    } catch (e: Exception) {
        context.getString(R.string.unknown_error)
    }
}

suspend fun checkAttendee(
    endpoint: String,
    token: String,
    context: Context,
    onError: (String) -> Unit = {},
    onSuccess: (String, String) -> Unit = {_,_ ->}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint,  token = token, context = context)
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
                        onError(context.getString(R.string.error_parsing_response) + ": ${e.message}")
                        return@withContext
                    }
                }
                400 -> {
                    onError(context.getString(R.string.error_400) + ": ${extractErrorMessage(response, context)}")
                }
                401 -> {
                    onError(context.getString(R.string.error_401))
                }
                403 -> {
                    onError(extractErrorMessage(response, context))
                }
                404 -> {
                    onError(context.getString(R.string.no_attendee_found))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": ${extractErrorMessage(response, context)}")
                }
            }
        } catch (e: Exception) {
            onError(context.getString(R.string.error) + ": ${e.message}")
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
    context: Context, 
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
            val response = httpPetition(endpoint, jsonString, context = context)
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
                    onError(context.getString(R.string.incorrect_login_credentials))
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $responseBody")
                }
            }
        } catch (e: Exception) {
            onError(context.getString(R.string.error) + ": ${e.message}")
        }
    }
}


@Serializable
data class CSRResponse(
    val signed_cert: String? = null,
    val ca_cert: String? = null
)


suspend fun sendCSR(
    endpoint: String,
    csrPem: String,
    context: Context,
    onError: (String) -> Unit = {},
    onSuccess: (CSRResponse) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val jsonBody = Json.encodeToString(mapOf("csr" to csrPem))
            val httpResponse = httpPetition(endpoint, jsonBody, context = context)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    val constructor_json = Json { ignoreUnknownKeys = true }
                    val cert = constructor_json.decodeFromString<CSRResponse>(body)
                    onSuccess(cert)
                }
                400 -> {
                    onError(context.getString(R.string.error_400) + ": ${extractErrorMessage(body, context)}")
                }
                403 -> {
                    onError(context.getString(R.string.error_403) + ": ${extractErrorMessage(body, context)}")
                }
                else -> {
                    onError(context.getString(R.string.unexpected_error) + ": $body")
                }
            }
        } catch (e: Exception) {
            onError(e.message.toString())
        }
    }
}

