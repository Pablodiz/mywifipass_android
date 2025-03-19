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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.net.HttpURLConnection 
import java.net.URL
import java.io.BufferedInputStream
import java.io.InputStream
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ExperimentalMaterial3Api

import com.example.get_eap_tls.ui.components.*
import com.example.get_eap_tls.backend.peticionHTTP

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
                    // Crear un estado mutable para almacenar la reply
                    var reply by remember { mutableStateOf("") }
                    var makePetition by remember { mutableStateOf(false) }
                    var showDialog by  remember { mutableStateOf(false) }
                    var textIngresado by remember { mutableStateOf("") }

                    // Ejecutar la petición cuando `makePetition` cambie a `true`
                    if (makePetition) {
                        LaunchedEffect(Unit) {
                            reply = withContext(Dispatchers.IO) {
                                peticionHTTP( URL("http://192.168.100.102:8080/prueba.txt"))
                            }
                            makePetition = false // Reiniciar el estado
                        }
                    }

                    Column {
                        Button(onClick = {
                            showDialog = true    
                            //makePetition.value = true // Activar la petición
                        }) {
                            Text(text = "Abrir diálogo")

                        }

                        // Mostrar la reply en la interfaz
                        Text(text = reply)
                    }
                    
                    MyDialog(
                        showDialog = showDialog, 
                        onDismiss = { 
                            makePetition = false 
                            showDialog = false
                        }, 
                        onAccept = { 
                            makePetition = false
                            Toast.makeText(this, "text ingresado: $textIngresado", Toast.LENGTH_SHORT).show()
                            showDialog = false 

                        },
                        content = {
                            MyTextField( 
                                onTextChange = { textIngresado = it }
                            )
                        }
                    )
                }
            }
        }
    }
}

