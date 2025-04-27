package com.example.get_eap_tls

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout

import com.example.get_eap_tls.ui.components.BackButton
import com.example.get_eap_tls.ui.components.QRScannerDialog
import com.example.get_eap_tls.ui.components.QrData
import android.widget.Toast
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.get_eap_tls.backend.api_petitions.allowAccess
import kotlinx.coroutines.withContext

@Composable 
fun AdminScreen(
    context: Context = LocalContext.current
){
    var showScannerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showScannerDialog) {
        QRScannerDialog(
            onDismiss = { 
                showScannerDialog = false 
            },
            onResult = { result ->
                var response = ""
                scope.launch {
                    withContext(Dispatchers.IO){
                        try {
                            val qrData = Json.decodeFromString<QrData>(result)
                            val endpoint = qrData.validation_url
                            val body = qrData.toBodyPetition()
                            val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                            val token = sharedPreferences.getString("auth_token", null) ?: throw IllegalStateException("Auth token is missing")
                            allowAccess(
                                endpoint = endpoint,
                                body = body, 
                                token = token,
                                onSuccess = { message -> 
                                    response = message
                                },
                                onError = { error -> 
                                    response = error
                                }
                            )
                        } catch (e: Exception) {
                            response = "Error: ${e.message}"
                        }
                    }
                    Toast.makeText(context, response, Toast.LENGTH_SHORT).show()
                }
                showScannerDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { showScannerDialog = true }) {
            Text("Open QR Scanner")
        }
    }
}

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetEAP_TLSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopEnd), 
                            onClick = { 
                                // Handle logout action
                                val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                                sharedPreferences.edit().remove("auth_token").apply()
                                finish() 
                            }
                        ){
                            Icon(Icons.Filled.Logout, contentDescription = "Logout")   
                        }
                        BackButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopStart),
                            onClick = { finish() }
                        )
                        AdminScreen(context)
                    }
                }
            }
        }
    }
}
