package com.example.get_eap_tls.backend

import java.net.HttpURLConnection 
import java.net.URL

data class HttpResponse(val statusCode: Int, val body: String)

suspend fun httpPetition(url_string: String, jsonString: String? = null, token: String? = null): HttpResponse {
    return try {
        val url = URL(url_string)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.connectTimeout = 5000
        urlConnection.readTimeout = 5000
        urlConnection.requestMethod = if (jsonString != null) "POST" else "GET"
        urlConnection.setRequestProperty("Accept", "application/json")
        urlConnection.setRequestProperty("Content-type", "application/json")
        urlConnection.doInput = true 

        if (token != null) {
            urlConnection.setRequestProperty("Authorization", "Token $token")
        }

        if (jsonString != null) {
            urlConnection.doOutput = true
            urlConnection.outputStream.use { it.write(jsonString.toByteArray()) }
        }

        val statusCode = urlConnection.responseCode // The petition is made here
        val responseBody = if (statusCode in 200..299) {
            urlConnection.inputStream.bufferedReader().use { it.readText() }
        } else {
            urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        HttpResponse(statusCode, responseBody)
    } catch (e: Exception) {
        HttpResponse(500, e.message ?: "Unknown Error")
    }
}