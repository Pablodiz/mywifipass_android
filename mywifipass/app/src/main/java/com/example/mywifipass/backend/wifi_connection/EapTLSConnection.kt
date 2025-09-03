package app.mywifipass.backend.wifi_connection

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiEnterpriseConfig.TLS_V1_2
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import app.mywifipass.backend.certificates.EapTLSCertificate

class EapTLSConnection(val ssid: String, eapTLSCertificate: EapTLSCertificate, identity: String, altSubjectMatch: String) {
    val suggestion: WifiNetworkSuggestion
    val wifiEnterpriseConfig: WifiEnterpriseConfig
    
    init{
        // Create EAP-TLS configuration
        wifiEnterpriseConfig = WifiEnterpriseConfig().apply {
            eapMethod = WifiEnterpriseConfig.Eap.TLS
            caCertificate = eapTLSCertificate.caCertificate
            setClientKeyEntry(eapTLSCertificate.clientPrivateKey, eapTLSCertificate.clientCertificate)
            setIdentity(identity)
            // Only set minimum TLS version on Android 14+ (API 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                minimumTlsVersion = TLS_V1_2
            }
            setAltSubjectMatch("DNS:"+altSubjectMatch)        
        }

        // Create WifiNetworkSuggestion
        suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
            .build()
    }

    fun connect(wifiManager: WifiManager, context: Context? = null){
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context != null -> {
                // Android 11+: Use Settings Intent API to ask user approval
                connectWithSettingsIntent(context)
            }
            else -> {
                // Android 10-: Use Wi-Fi Suggestion API
                connectWithSuggestion(wifiManager)
            }        
        }
    }
    
    private fun connectWithSuggestion(wifiManager: WifiManager) {
        wifiManager.addNetworkSuggestions(listOf(suggestion))   
    }
    
    private fun connectWithSettingsIntent(context: Context) {
        // Create intent to add network via Settings
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
            putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
        }
        context.startActivity(intent)
    }
    
    fun disconnect(wifiManager: WifiManager, context: Context){
         when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R && context != null -> {
                wifiManager.removeNetworkSuggestions(listOf(suggestion))
            }
         }
    }
}