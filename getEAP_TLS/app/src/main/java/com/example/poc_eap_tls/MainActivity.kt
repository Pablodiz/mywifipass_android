package com.example.get_eap_tls

import android.content.Context
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiEnterpriseConfig.TLS_V1_2
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.get_eap_tls.ui.theme.getEAP_TLSTheme
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class MainActivity : ComponentActivity() {
    @RequiresApi(34)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            getEAP_TLSTheme {
                // A surface container using the 'background' color from the theme  
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Obtengo los ficheros (que ahora están en getEAP_TLS\app\src\main\res\raw)  
                    val caInputStream = resources.openRawResource(R.raw.ca)
                    val p12InputStream = resources.openRawResource(R.raw.tres_juntos)
                    val clientInputStream = resources.openRawResource(R.raw.client)
                    val password = "whatever"
                    // Obtengo el certificado x509  
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val caCertificate =
                        certificateFactory.generateCertificate(caInputStream) as X509Certificate

                    // Obtengo la clave y certificado x509 de usuario  
                    val keyStore = KeyStore.getInstance("PKCS12")
                    keyStore.load(clientInputStream, "whatever".toCharArray())
                    val alias = keyStore.aliases().nextElement()
                    val clientCertificate = keyStore.getCertificate(alias) as X509Certificate
                    val clientPrivateKey =
                        keyStore.getKey(alias, "password".toCharArray()) as PrivateKey

                    // Creamos la config de EAP-TLS  
                    val wifiEnterpriseConfig = WifiEnterpriseConfig()
                    wifiEnterpriseConfig.eapMethod = WifiEnterpriseConfig.Eap.TLS
                    wifiEnterpriseConfig.caCertificate = caCertificate
                    wifiEnterpriseConfig.setClientKeyEntry(clientPrivateKey, clientCertificate)
                    wifiEnterpriseConfig.identity = "Prueba"
                    wifiEnterpriseConfig.minimumTlsVersion = TLS_V1_2
                    wifiEnterpriseConfig.domainSuffixMatch = "Example Server Certificate"


                    // Crear sugerencia de red
                    val suggestion = WifiNetworkSuggestion.Builder()
                        .setSsid("OpenWrt_TLS")
                        .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
                        .build()

                    val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

                    Column {
                        Button(onClick = { wifiManager.addNetworkSuggestions(listOf(suggestion)) }) {
                            Text(text = "Conectar a red")
                        }
                        Button(onClick = { wifiManager.removeNetworkSuggestions(listOf(suggestion)) }) {
                            Text(text = "Olvidar red")
                        }
                    }
                }
            }
        }
    }
}