package app.mywifipass.backend.certificates

import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.io.InputStream
import java.security.cert.CertificateFactory
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

// Imports for the AES decryption
import javax.crypto.Cipher
import javax.crypto.SecretKey
import android.util.Base64
import javax.crypto.spec.SecretKeySpec

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
        return Base64.decode(base64Content, Base64.DEFAULT)
    }
}

// fun hexToSecretKey(hex: String): SecretKey {
//     if (hex.isEmpty()) {
//         throw IllegalArgumentException("Hex string cannot be empty")
//     }
//     if (!hex.matches(Regex("^[0-9a-fA-F]+$"))) {
//         throw IllegalArgumentException("Invalid hex string format: $hex")
//     }

//     return try {
//         val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
//         SecretKeySpec(bytes, 0, bytes.size, "AES")
//     } catch (e: NumberFormatException) {
//         throw IllegalArgumentException("Failed to parse hex string: $hex", e)
//     } catch (e: Exception) {
//         throw RuntimeException("Unexpected error while converting hex to SecretKey", e)
//     }
// }

// fun decryptAES256(
//     encryptedText: String,
//     secretKey: SecretKey,
// ): String {
//     val textToDecrypt = Base64.decode(encryptedText, Base64.DEFAULT)
//     val cipher = Cipher.getInstance("AES/ECB/PKCS7PADDING")
//     cipher.init(Cipher.DECRYPT_MODE, secretKey)

//     val decrypt = cipher.doFinal(textToDecrypt)
//     return String(decrypt)
// }

fun checkCertificate(certificate: String) : Boolean{
    return (certificate.startsWith("-----BEGIN CERTIFICATE-----") && certificate.endsWith("-----END CERTIFICATE-----\n"))
}

fun checkPrivateKey(privateKey: String): Boolean{
    return (privateKey.startsWith("-----BEGIN PRIVATE KEY-----") && privateKey.endsWith("-----END PRIVATE KEY-----\n"))
}

fun checkCertificates(ca_certificate: String, client_certificate: String, client_private_key: String) : Boolean{
    return checkCertificate(ca_certificate) && checkCertificate(client_certificate) && checkPrivateKey(client_private_key)
}