package app.mywifipass.backend.certificates

/// CSRs
import java.security.KeyPairGenerator
import java.security.KeyPair
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter

fun csrToPem(csr: PKCS10CertificationRequest): String {
    val stringWriter = StringWriter()
    val pemWriter = PemWriter(stringWriter)
    pemWriter.use {
        it.writeObject(org.bouncycastle.util.io.pem.PemObject("CERTIFICATE REQUEST", csr.encoded))
    }
    return stringWriter.toString()
}

fun generateCSR(keyPair: KeyPair, commonName: String): PKCS10CertificationRequest {
    val subject = X500Name("CN=$commonName")
    val signGen = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
    return JcaPKCS10CertificationRequestBuilder(subject, keyPair.public).build(signGen)
}

fun generateKeyPair(): KeyPair {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(2048)
    return keyGen.generateKeyPair()
}