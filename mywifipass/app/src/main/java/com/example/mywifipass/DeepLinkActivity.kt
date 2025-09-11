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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign

import app.mywifipass.ui.theme.MyWifiPassTheme

// URL decoding 
import android.net.Uri 
import java.net.URLDecoder

class DeepLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener la URL que activó esta Activity
        val incomingUri = intent.data?.toString() ?: "No URI received"
        val parsedUri = Uri.parse(incomingUri)
        val wifipassUrl = parsedUri.getQueryParameter("url")
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeepLinkScreen(wifipass_url = wifipassUrl)
                }
            }
        }
    }
}

@Composable
fun DeepLinkScreen(wifipass_url: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Deep Link Received",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
             
        Text(
            text = "URL in the url parameter:",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        

        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = wifipass_url ?: "No url parameter found",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "This screen shows the URL that was used to open the app via deep linking.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
