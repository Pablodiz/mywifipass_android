/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.Intent
import app.mywifipass.backend.extractURLFromParameter

class DeepLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener la URL que activó esta Activity
        val wifipassUrl = extractURLFromParameter(intent.data?.toString() ?: "")
        
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
