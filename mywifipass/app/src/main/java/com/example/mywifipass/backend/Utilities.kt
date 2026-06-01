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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException

import android.content.Context
import app.mywifipass.R
import android.net.Uri


data class HttpResponse(val statusCode: Int, val body: String)

enum class SseOutcome {
    AUTHORIZED,
    TIMEOUT,
    ERROR,
    DISCONNECTED,
    UNAVAILABLE,
}

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
    } catch (e: SocketTimeoutException) {
        HttpResponse(504, context.getString(R.string.connection_timeout))
    } catch (e: UnknownHostException) {
        HttpResponse(0, context.getString(R.string.no_internet_connection))
    } catch (e: ConnectException) {
        HttpResponse(0, context.getString(R.string.no_internet_connection))
    } catch (e: Exception) {
        HttpResponse(500, e.message ?: context.getString(R.string.unknown_error))
    }
}

suspend fun ssePetition(url_string: String, token: String? = null, onEvent: (eventName: String, data: String) -> Unit, context: Context): SseOutcome {
    return try {
        val sseUrl = if (url_string.contains("?")) {
            "$url_string&stream=1"
        } else {
            "$url_string?stream=1"
        }
        val url = URL(sseUrl)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.connectTimeout = 5000
        // Keep read timeout comfortably above backend heartbeat cadence.
        urlConnection.readTimeout = 45000
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Accept", "text/event-stream")
        urlConnection.setRequestProperty("Cache-Control", "no-cache")
        urlConnection.setRequestProperty("Connection", "keep-alive")
        urlConnection.doInput = true

        if (token != null) {
            urlConnection.setRequestProperty("Authorization", "Token $token")
        }

        val statusCode = urlConnection.responseCode
        if (statusCode !in 200..299) {
            return SseOutcome.UNAVAILABLE
        }

        var streamWasAlive = false

        urlConnection.inputStream.bufferedReader().use { reader ->
            var currentEventName: String? = null
            val currentData = StringBuilder()

            while (true) {
                val line = reader.readLine() ?: break

                when {
                    line.startsWith("event:") -> {
                        currentEventName = line.substringAfter("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        if (currentData.isNotEmpty()) {
                            currentData.append('\n')
                        }
                        currentData.append(line.substringAfter("data:").trim())
                    }
                    line.isBlank() -> {
                        val eventName = currentEventName ?: "message"
                        streamWasAlive = true
                        onEvent(eventName, currentData.toString())
                        when (eventName) {
                            "authorized" -> return SseOutcome.AUTHORIZED
                            "timeout" -> return SseOutcome.TIMEOUT
                            "error" -> return SseOutcome.ERROR
                        }
                        currentEventName = null
                        currentData.clear()
                    }
                }
            }
        }

        if (streamWasAlive) SseOutcome.DISCONNECTED else SseOutcome.UNAVAILABLE
    } catch (e: Exception) {
        SseOutcome.UNAVAILABLE
    }
}


fun extractURLFromParameter(incomingUri: String): String {
    val parsedUri = Uri.parse(incomingUri)
    return parsedUri.getQueryParameter("url") ?: ""
}