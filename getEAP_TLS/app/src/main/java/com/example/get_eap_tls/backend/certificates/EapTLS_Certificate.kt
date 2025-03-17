package com.example.get_eap_tls.backend.certificates

import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.KeyStore


class EapTLSCertificate(caInputStream: InputStream,  clientInputStream: InputStream, val privateKeyPassword: String) {
    val caCertificate: X509Certificate
    val clientPrivateKey: PrivateKey
    val clientCertificate: X509Certificate
    init{
        // Obtengo el certificado x509  
        val certificateFactory = CertificateFactory.getInstance("X.509")
        caCertificate =
            certificateFactory.generateCertificate(caInputStream) as X509Certificate

        // Obtengo la clave y certificado x509 de usuario  
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(clientInputStream, privateKeyPassword.toCharArray())
        val alias = keyStore.aliases().nextElement()
        clientCertificate = keyStore.getCertificate(alias) as X509Certificate
        clientPrivateKey = keyStore.getKey(alias, "password".toCharArray()) as PrivateKey
    }
}