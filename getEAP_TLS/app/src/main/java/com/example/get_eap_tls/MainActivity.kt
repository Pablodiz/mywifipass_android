package com.example.get_eap_tls

import android.content.Context
import android.net.wifi.WifiManager
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
import com.example.get_eap_tls.backend.wifi_connection.EapTLSConnection
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate


import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.net.HttpURLConnection 
import java.net.URL
import java.io.BufferedInputStream
import java.io.InputStream

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
                    // // Obtengo los ficheros (que ahora están en getEAP_TLS\app\src\main\res\raw)  
                    // val caInputStream = resources.openRawResource(R.raw.ca)
                    // val clientInputStream = resources.openRawResource(R.raw.client)
                    // val privateKeyPassword = "whatever"
                    
                    // val eapTLSCertificate = EapTLSCertificate(caInputStream, clientInputStream, privateKeyPassword)

                    // val eapTLSConnection = EapTLSConnection("OpenWrt_TLS", eapTLSCertificate)

                    // val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager



                    // Column {
                    //     Button(onClick = { eapTLSConnection.connect(wifiManager) }) {
                    //         Text(text = "Conectar a red")
                    //     }
                    //     Button(onClick = { eapTLSConnection.disconnect(wifiManager) }) {
                    //         Text(text = "Olvidar red")
                    //     }
                    // }
                    // Crear un estado mutable para almacenar la respuesta
                    // Crear un estado mutable para almacenar la respuesta
                    val respuesta = remember { mutableStateOf("") }
                    val hacerPeticion = remember { mutableStateOf(false) }

                    // Ejecutar la petición cuando `hacerPeticion` cambie a `true`
                    if (hacerPeticion.value) {
                        LaunchedEffect(Unit) {
                            respuesta.value = withContext(Dispatchers.IO) {
                                petition( URL("http://192.168.100.102:8080/prueba.txt"))
                            }
                            hacerPeticion.value = false // Reiniciar el estado
                        }
                    }

                    Column {
                        Button(onClick = {
                            hacerPeticion.value = true // Activar la petición
                        }) {
                            Text(text = "Hacer petición")
                        }
                        // Mostrar la respuesta en la interfaz
                        Text(text = respuesta.value)
                    }
                }
            }
        }
    }
}

// Hay que llamarlo desde un thread separado
suspend fun petition(url: URL): String {
    val urlConnection = url.openConnection() as HttpURLConnection
    return try {
        urlConnection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Log.e("HTTP Error", "Error: ${e::class.simpleName} - ${e.message}")
        "Error: ${e::class.simpleName} - ${e.message}"
    } finally {
        urlConnection.disconnect()
    }
}