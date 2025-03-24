package com.example.get_eap_tls.backend

import java.net.HttpURLConnection 
import java.net.URL

suspend fun peticionHTTP(url_string: String): String {
    val url = URL(url_string)
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.setRequestProperty("Accept", "application/json")
    return try {
        urlConnection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        "Error: ${e::class.simpleName} - ${e.message}"
    } finally {
        urlConnection.disconnect()
    }
}