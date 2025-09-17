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
import android.util.Log
import android.widget.Toast
import android.os.Handler
import android.os.Looper

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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK // Add flag for non-Activity context
        }
        context.startActivity(intent)
    }
    
    fun disconnect(wifiManager: WifiManager, context: Context) {
         when {
            // Android 10-: Remove suggestion directly
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R && context != null -> {
                wifiManager.removeNetworkSuggestions(listOf(suggestion))
                Log.d("EapTLSConnection", "Removed network suggestion for SSID: $ssid")
            }
            else -> {
                // Android 11+: Cannot remove suggestion added via Settings Intent
                Log.d("EapTLSConnection", "Network must be removed manually from WiFi settings on Android 11+")
                
                // Show Toast on main thread
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "You may forget the network in settings", Toast.LENGTH_LONG).show()
                }

                // Open WiFi settings for user to remove manually
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK // Add flag for non-Activity context
                }
                context.startActivity(intent)
            }
         }
    }
}