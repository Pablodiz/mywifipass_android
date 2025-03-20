package com.example.get_eap_tls.backend.certificates

import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64


class EapTLSCertificate(caInputStream: InputStream,  
                        clientCertInputStream: InputStream, 
                        clientKeyInputStream: InputStream) {
    val caCertificate: X509Certificate
    val clientPrivateKey: PrivateKey
    val clientCertificate: X509Certificate
    init {
        // Obtengo el certificado CA (X.509)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        caCertificate = certificateFactory.generateCertificate(caInputStream) as X509Certificate

        // Obtengo el certificado del cliente (X.509)
        clientCertificate = certificateFactory.generateCertificate(clientCertInputStream) as X509Certificate

        // Obtengo la clave privada del cliente
        val keyBytes = clientKeyInputStream.readBytes()
        val keySpec = PKCS8EncodedKeySpec(decodePem(keyBytes))
        val keyFactory = KeyFactory.getInstance("RSA")
        clientPrivateKey = keyFactory.generatePrivate(keySpec)
    }

    // Función para decodificar un archivo PEM si tiene encabezados y pies de página
    private fun decodePem(pem: ByteArray): ByteArray {
        val pemString = String(pem)
        val base64Content = pemString
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        return Base64.getDecoder().decode(base64Content)
    }
}


fun procesarPeticionCertificados(json: String){



}