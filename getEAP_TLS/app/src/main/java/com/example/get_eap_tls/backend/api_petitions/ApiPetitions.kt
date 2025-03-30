package com.example.get_eap_tls.backend.api_petitions

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate

@Serializable
data class ParsedReply(
    val user_name: String,
    val user_email: String,
    val user_id_document: String,
    val ca_certificate: String, 
    val certificate: String, 
    val private_key: String,
    val ssid: String, 
    val network_common_name: String,
)

data class WifiNetworkLocation(
    val certificates: EapTLSCertificate,
    val fullParsedReply: ParsedReply,
)


fun parseReply(string: String): ParsedReply{
    return try{
        val constructor_json = Json { ignoreUnknownKeys = true }
        constructor_json.decodeFromString<ParsedReply>(string)
    } catch (e: Exception){
        throw Exception("Error al procesar la respuesta: ${e.message}")
    }
}


fun processReply(string: String): WifiNetworkLocation{
    return try{
        val parsedReply = parseReply(string)
        val certificates = EapTLSCertificate(
            parsedReply.ca_certificate.byteInputStream(),
            parsedReply.certificate.byteInputStream(),
            parsedReply.private_key.byteInputStream()
        )
        WifiNetworkLocation(certificates = certificates, fullParsedReply = parsedReply)

    } catch (e: Exception){
        throw Exception("Error al procesar el : ${e.message}")
    }
}



class DataSource(){
    fun loadConnections(): List<WifiNetworkLocation>{
        var parsedReply = ParsedReply(
            user_name = "user_name",
            user_email = "user_email",
            user_id_document = "user_id_document",
            ca_certificate = "ca_certificate", 
            certificate = "certificate", 
            private_key = "private_key",
            ssid = "ssid", 
            network_common_name = "network_common_name"
    )
        //Removed values so that gitguardian doesnt implode
        val CA_CERTIFICATE = ""
        val PRIVATE_KEY = "" 
        val CLIENT_CERTIFICATE = ""
        
        return listOf<WifiNetworkLocation>(
            WifiNetworkLocation(
                certificates = EapTLSCertificate(
                    caInputStream = CA_CERTIFICATE.byteInputStream(),
                    clientCertInputStream = CLIENT_CERTIFICATE.byteInputStream(),
                    clientKeyInputStream = PRIVATE_KEY.byteInputStream()
                ),
                fullParsedReply = parsedReply
            ),WifiNetworkLocation(
                certificates = EapTLSCertificate(
                    caInputStream = CA_CERTIFICATE.byteInputStream(),
                    clientCertInputStream = CLIENT_CERTIFICATE.byteInputStream(),
                    clientKeyInputStream = PRIVATE_KEY.byteInputStream()
                ),
                fullParsedReply = parsedReply
            ),WifiNetworkLocation(
                certificates = EapTLSCertificate(
                    caInputStream = CA_CERTIFICATE.byteInputStream(),
                    clientCertInputStream = CLIENT_CERTIFICATE.byteInputStream(),
                    clientKeyInputStream = PRIVATE_KEY.byteInputStream()
                ),
                fullParsedReply = parsedReply
            ),WifiNetworkLocation(
                certificates = EapTLSCertificate(
                    caInputStream = CA_CERTIFICATE.byteInputStream(),
                    clientCertInputStream = CLIENT_CERTIFICATE.byteInputStream(),
                    clientKeyInputStream = PRIVATE_KEY.byteInputStream()
                ),
                fullParsedReply = parsedReply
            ),WifiNetworkLocation(
                certificates = EapTLSCertificate(
                    caInputStream = CA_CERTIFICATE.byteInputStream(),
                    clientCertInputStream = CLIENT_CERTIFICATE.byteInputStream(),
                    clientKeyInputStream = PRIVATE_KEY.byteInputStream()
                ),
                fullParsedReply = parsedReply
            )
        )
    }
    
}