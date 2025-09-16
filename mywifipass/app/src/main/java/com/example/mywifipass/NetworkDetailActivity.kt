package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.ui.zIndex
import android.content.Intent
import android.content.Context

import app.mywifipass.ui.theme.MyWifiPassTheme
import app.mywifipass.ui.components.BackButton
import app.mywifipass.ui.components.NetworkDetailScreen

import app.mywifipass.controller.MainController


class NetworkDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get the network_id from the intent
        val networkId = intent.getIntExtra("network_id", -1)
        
        if (networkId == -1) {
            // If there is no valid network ID, close the activity
            finish()
            return
        }
        
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val wifiManager = remember { applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager }
                    val mainController = remember { MainController(applicationContext) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NetworkDetailScreen(
                            selectedNetworkId = networkId,
                            modifier = Modifier.fillMaxSize(),
                            wifiManager = wifiManager,
                            mainController = mainController,
                            onNavigateBack = { 
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Static method for starting this Dialog
    companion object {
        fun start(context: Context, networkId: Int) {
            val intent = Intent(context, NetworkDetailActivity::class.java).apply {
                putExtra("network_id", networkId)
            }
            context.startActivity(intent)
        }
    }
}
