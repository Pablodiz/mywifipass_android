package com.example.get_eap_tls.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate

import com.example.get_eap_tls.backend.database.Network
import com.example.get_eap_tls.backend.database.DataSource
import com.example.get_eap_tls.backend.httpPetition
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
            val reply = httpPetition(enteredText)
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
    endpoint: String
):String {
    var response = ""
    withContext(Dispatchers.IO) {
        try {
            val reply = httpPetition(endpoint)
            val constructor_json = Json { ignoreUnknownKeys = true }
            val symmetricKeyJSON = constructor_json.decodeFromString<CertificatesSymmetricKey>(reply)
            response = symmetricKeyJSON.certificates_symmetric_key
        } catch (e: Exception) {
            response = e.message.toString()
        }
    }
    return response
}