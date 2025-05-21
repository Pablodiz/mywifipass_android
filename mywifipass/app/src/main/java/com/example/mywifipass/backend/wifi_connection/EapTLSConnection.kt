package app.mywifipass.backend.wifi_connection

import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiEnterpriseConfig.TLS_V1_2
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import app.mywifipass.backend.certificates.EapTLSCertificate

class EapTLSConnection(val ssid: String, eapTLSCertificate: EapTLSCertificate, identity: String, domainSuffixMatch: String) {
    val suggestion: WifiNetworkSuggestion
    val wifiEnterpriseConfig: WifiEnterpriseConfig
    init{
        // Creamos la config de EAP-TLS  
        wifiEnterpriseConfig = WifiEnterpriseConfig()
        wifiEnterpriseConfig.eapMethod = WifiEnterpriseConfig.Eap.TLS
        wifiEnterpriseConfig.caCertificate = eapTLSCertificate.caCertificate
        wifiEnterpriseConfig.setClientKeyEntry(eapTLSCertificate.clientPrivateKey, eapTLSCertificate.clientCertificate)
        wifiEnterpriseConfig.identity = identity
        wifiEnterpriseConfig.minimumTlsVersion = TLS_V1_2
        wifiEnterpriseConfig.domainSuffixMatch = domainSuffixMatch

        // Crear sugerencia de red
        suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
            .build()


    }

    fun connect(wifiManager: WifiManager){
        wifiManager.addNetworkSuggestions(listOf(suggestion))   
    }         

    fun disconnect(wifiManager: WifiManager){
        wifiManager.removeNetworkSuggestions(listOf(suggestion))
    }
}

