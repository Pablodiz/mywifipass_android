package com.example.get_eap_tls.backend

import java.net.HttpURLConnection 
import java.net.URL

data class HttpResponse(val statusCode: Int, val body: String)

suspend fun httpPetition(url_string: String, jsonString: String? = null): HttpResponse {
    return try {
        val url = URL(url_string)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = if (jsonString != null) "POST" else "GET"
        urlConnection.setRequestProperty("Accept", "application/json")
        urlConnection.setRequestProperty("Content-type", "application/json")
        urlConnection.doInput = true 

        if (jsonString != null) {
            urlConnection.doOutput = true
            urlConnection.outputStream.use { it.write(jsonString.toByteArray()) }
        }

        val statusCode = urlConnection.responseCode
        val responseBody = urlConnection.inputStream.bufferedReader().use { it.readText() }
        HttpResponse(statusCode, responseBody)
    } catch (e: Exception) {
        HttpResponse(500, e.message.toString() ?: "Unkown Error")
    }
}