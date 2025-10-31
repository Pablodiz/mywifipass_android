/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import app.mywifipass.backend.certificates.EapTLSCertificate
import app.mywifipass.backend.certificates.generateKeyPair
import app.mywifipass.backend.certificates.generateCSR
import app.mywifipass.backend.certificates.csrToPem
import android.os.Build

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
import android.util.Log

// Data classes for structured error/success callbacks
data class ApiResult(
    val title: String,
    val message: String,
    val isSuccess: Boolean = true,
    val errorCode: Int? = null,
    val showTrace: Boolean = false,
    val fullTrace: String? = null
)

// Callback types for API responses
typealias ApiSuccessCallback = (ApiResult) -> Unit
typealias ApiErrorCallback = (ApiResult) -> Unit

// Functions for handling different types of errors

private fun handleUnexpectedError(statusCode: Int, body: String, context: Context, operation: String): ApiResult {
    val fullTrace = "Operation: $operation\nStatus Code: $statusCode\nResponse Body: $body\nTimestamp: ${System.currentTimeMillis()}"
    
    
    
    return ApiResult(
        title = context.getString(R.string.unexpected_error_title),
        message = context.getString(R.string.server_error_message),
        isSuccess = false,
        errorCode = statusCode,
        showTrace = true,
        fullTrace = fullTrace
    )
}

private fun handleNetworkException(exception: Exception, context: Context, operation: String): ApiResult {
    val fullTrace = "Operation: $operation\nException: ${exception.javaClass.simpleName}\nMessage: ${exception.message}\nStackTrace: ${exception.stackTraceToString()}\nTimestamp: ${System.currentTimeMillis()}"
    
    return ApiResult(
        title = context.getString(R.string.network_error_title), 
        message = context.getString(R.string.network_connection_error),
        isSuccess = false,
        errorCode = null,
        showTrace = true,
        fullTrace = fullTrace
    )
}

fun parseReply(string: String, context: Context): Network{
    return try{
        Log.d("ApiPetitions", "Parsing response body: $string")
        
        // First try to parse as JSON to see if it's valid JSON
        val json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        
        // Try to parse as JSON object first to understand the structure
        try {
            val jsonElement = json.parseToJsonElement(string)
            Log.d("ApiPetitions", "JSON structure: $jsonElement")
        } catch (jsonException: Exception) {
            Log.e("ApiPetitions", "Not valid JSON: $string", jsonException)
            throw Exception(context.getString(R.string.invalid_json_format_error) + ": ${jsonException.message}")
        }
        
        json.decodeFromString<Network>(string)
    } catch (e: Exception){
        Log.e("ApiPetitions", "Failed to parse response: $string", e)
        
        // Check if the response might be HTML (404 page)
        if (string.contains("<html>") || string.contains("<!DOCTYPE")) {
            throw Exception(context.getString(R.string.server_returned_html_error))
        }
        
        throw Exception(context.getString(R.string.unrecognized_response_format) + ": ${e.message}")
    }
}

suspend fun confirmDownload(
    has_downloaded_url: String,
    context: Context,
    onError: ApiErrorCallback = {},
    onSuccess: ApiSuccessCallback = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(has_downloaded_url, jsonString = "", context = context)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess(ApiResult(
                        title = context.getString(R.string.success_200_confirm_download_title),
                        message = context.getString(R.string.success_200_confirm_download_message)
                    ))
                }
                404 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_confirm_download_title),
                        message = context.getString(R.string.error_404_confirm_download_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, body, context, "Download Confirmation"))
                }
            }
        } catch (e: Exception) {
            onError(handleNetworkException(e, context, "Download Confirmation"))
        }
    }
}

suspend fun downloadWifiPass(
    enteredText: String,
    dataSource: DataSource,
    context: Context, 
    onSuccess: ApiSuccessCallback = {},
    onError: ApiErrorCallback = {}
):List<Network> {
    withContext(Dispatchers.IO) {
        try{
            Log.d("ApiPetitions", "Making request to: $enteredText")
            val reply = httpPetition(enteredText, context = context)
            val body = reply.body
            val statusCode = reply.statusCode
            Log.d("ApiPetitions", "Response status: $statusCode, body: $body")
            when (statusCode) {
                200 -> {
                    val network = parseReply(body, context)
                    dataSource.insertNetwork(network)
                    confirmDownload(network.has_downloaded_url, context)
                    onSuccess(ApiResult(
                        title = context.getString(R.string.success_200_add_network_title),
                        message = context.getString(R.string.success_200_add_network_message)
                    )) 
                }
                403 -> {
                    val titleString = context.getString(R.string.error_403_add_network_title)
                    val messageString = context.getString(R.string.error_403_add_network_message)
                    Log.d("ApiPetitions", "String resources - Title: '$titleString', Message: '$messageString'")
                    onError(ApiResult(
                        title = context.getString(R.string.error_403_add_network_title),
                        message = context.getString(R.string.error_403_add_network_message),
                        isSuccess = false,
                        errorCode = 403
                    ))
                }
                404 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_add_network_title),
                        message = context.getString(R.string.error_404_add_network_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, body, context, "Add Network to Database"))
                }
            }

        } catch (e:Exception){
            onError(handleNetworkException(e, context, "Add Network to Database"))
        }
    }
    return dataSource.loadConnections()
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
    onError: ApiErrorCallback = {},
    onSuccess: ApiSuccessCallback = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val httpResponse = httpPetition(url_string = endpoint, jsonString="", token = token, context = context)
            val statusCode = httpResponse.statusCode
            val body = httpResponse.body
            when (statusCode) {
                200 -> {
                    onSuccess(ApiResult(
                        title = context.getString(R.string.success_200_authorize_attendee_title),
                        message = context.getString(R.string.success_200_authorize_attendee_message)
                    ))
                }
                400 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_400_authorize_attendee_title),
                        message = context.getString(R.string.error_400_authorize_attendee_message),
                        isSuccess = false,
                        errorCode = 400
                    ))
                }
                401 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_401_authorize_attendee_title),
                        message = context.getString(R.string.error_401_authorize_attendee_message),
                        isSuccess = false,
                        errorCode = 401
                    ))
                }
                403 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_403_authorize_attendee_title),
                        message = context.getString(R.string.error_403_authorize_attendee_message),
                        isSuccess = false,
                        errorCode = 403
                    ))
                }
                404 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_authorize_attendee_title),
                        message = context.getString(R.string.error_404_authorize_attendee_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, body, context, "Authorize Attendee"))
                }
            }
        } catch (e: Exception) {
            onError(handleNetworkException(e, context, "Authorize Attendee"))
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
    onError: ApiErrorCallback = {},
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
                        onError(handleNetworkException(e, context, "Check Attendee - Parse Response"))
                        return@withContext
                    }
                }
                400 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_400_check_attendee_title),
                        message = context.getString(R.string.error_400_check_attendee_message),
                        isSuccess = false,
                        errorCode = 400
                    ))
                }
                401 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_401_check_attendee_title),
                        message = context.getString(R.string.error_401_check_attendee_message),
                        isSuccess = false,
                        errorCode = 401
                    ))
                }
                403 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_403_check_attendee_title),
                        message = context.getString(R.string.error_403_check_attendee_message),
                        isSuccess = false,
                        errorCode = 403
                    ))
                }
                404 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_check_attendee_title),
                        message = context.getString(R.string.error_404_check_attendee_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, response, context, "Check Attendee"))
                }
            }
        } catch (e: Exception) {
            onError(handleNetworkException(e, context, "Check Attendee"))
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
    onError: ApiErrorCallback = {}, 
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
                    onError(ApiResult(
                        title = context.getString(R.string.error_400_login_petition_title),
                        message = context.getString(R.string.error_400_login_petition_message),
                        isSuccess = false,
                        errorCode = 400
                    ))
                }
                401 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_401_login_petition_title),
                        message = context.getString(R.string.error_401_login_petition_message),
                        isSuccess = false,
                        errorCode = 401
                    ))
                }
                403 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_403_login_petition_title),
                        message = context.getString(R.string.error_403_login_petition_message),
                        isSuccess = false,
                        errorCode = 403
                    ))
                }
                404 -> {
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_login_petition_title),
                        message = context.getString(R.string.error_404_login_petition_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, responseBody, context, "Login"))
                }
            }
        } catch (e: Exception) {
            onError(handleNetworkException(e, context, "Login"))
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
    token: String,
    context: Context,
    onError: ApiErrorCallback = {},
    onSuccess: (CSRResponse) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        try {
            val androidVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()
            val jsonBody = Json.encodeToString(mapOf("csr" to csrPem, "token" to token, "androidVersion" to androidVersion))
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
                    Log.d("ApiPetitions", "CSR response error: ${extractErrorMessage(body, context)}")
                    onError(ApiResult(
                        title = context.getString(R.string.error_400_send_csr_title),
                        message = context.getString(R.string.error_400_send_csr_message),
                        isSuccess = false,
                        errorCode = 400
                    ))
                }
                401 -> {
                    Log.d("ApiPetitions", "CSR response error: ${extractErrorMessage(body, context)}")
                    onError(ApiResult(
                        title = context.getString(R.string.error_401_send_csr_title),
                        message = context.getString(R.string.error_401_send_csr_message),
                        isSuccess = false,
                        errorCode = 401
                    ))
                }
                403 -> {
                    Log.d("ApiPetitions", "CSR response error: ${extractErrorMessage(body, context)}")
                    onError(ApiResult(
                        title = context.getString(R.string.error_403_send_csr_title),
                        message = context.getString(R.string.error_403_send_csr_message),
                        isSuccess = false,
                        errorCode = 403
                    ))
                }
                404 -> {
                    Log.d("ApiPetitions", "CSR response error: ${extractErrorMessage(body, context)}")
                    onError(ApiResult(
                        title = context.getString(R.string.error_404_send_csr_title),
                        message = context.getString(R.string.error_404_send_csr_message),
                        isSuccess = false,
                        errorCode = 404
                    ))
                }
                else -> {
                    onError(handleUnexpectedError(statusCode, body, context, "Send CSR"))
                }
            }
        } catch (e: Exception) {
            onError(handleNetworkException(e, context, "Send CSR"))
        }
    }
}


suspend fun checkUserAuthorized(
    endpoint: String,
    context: Context,
    onError: ApiErrorCallback = {},
    onSuccess: (Boolean) -> Unit = {}
){
    val httpResponse = httpPetition(endpoint, context = context)
    val statusCode = httpResponse.statusCode
    val body = httpResponse.body
    when (statusCode) {
        200 -> {
            val authorized = true 
            onSuccess(authorized)
        }
        403 -> {
            val authorized = false 
            onSuccess(authorized)
        }
        404 -> {
            onError(ApiResult(
                title = context.getString(R.string.error_404_check_user_authorized_title),
                message = context.getString(R.string.error_404_check_user_authorized_message),
                isSuccess = false,
                errorCode = 404
            ))
        }
        else -> {
            onError(handleUnexpectedError(statusCode, body, context, "Check User Authorization"))
        }
    }
}