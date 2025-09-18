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

import app.mywifipass.ui.theme.MyWifiPassTheme
import app.mywifipass.ui.components.MainScreenContainer
import app.mywifipass.ui.components.TopBar

// i18n
import androidx.compose.ui.res.stringResource
import app.mywifipass.R 

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener la URL del pase WiFi si viene del deep link
        val wifiPassUrl = intent.getStringExtra("wifi_pass_url")
        
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TopBar(
                            title = stringResource(R.string.downloaded_wifi_passes),
                            onBackClick = { finish() },
                        )
                        MainScreenContainer(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp),
                            initialWifiPassUrl = wifiPassUrl
                        )
                    }
                }
            }
        }
    }
}