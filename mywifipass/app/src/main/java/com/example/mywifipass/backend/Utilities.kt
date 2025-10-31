/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.backend

import java.net.HttpURLConnection 
import java.net.URL

import android.content.Context
import androidx.compose.ui.res.stringResource
import app.mywifipass.R
import android.net.Uri  


data class HttpResponse(val statusCode: Int, val body: String)

suspend fun httpPetition(url_string: String, jsonString: String? = null, token: String? = null, context: Context): HttpResponse {
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
        HttpResponse(500, e.message ?: context.getString(R.string.unknown_error)) // Return 500 on exception
    }
}


fun extractURLFromParameter(incomingUri: String): String {
    val parsedUri = Uri.parse(incomingUri)
    return parsedUri.getQueryParameter("url") ?: ""
}