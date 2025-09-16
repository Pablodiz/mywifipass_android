package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri

class DeepLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener la URL que activó esta Activity
        val incomingUri = intent.data?.toString() ?: ""
        val parsedUri = Uri.parse(incomingUri)
        val wifipassUrl = parsedUri.getQueryParameter("url")
        
        // Crear Intent para MainActivity
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            // Pasar la URL como extra si existe
            if (!wifipassUrl.isNullOrEmpty()) {
                putExtra("wifi_pass_url", wifipassUrl)
            }
        }
        
        // Iniciar MainActivity y cerrar esta Activity
        startActivity(mainIntent)
        finish()
    }
}
