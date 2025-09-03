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
import app.mywifipass.ui.theme.MyWifiPassTheme
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout

import app.mywifipass.ui.components.BackButton
import app.mywifipass.ui.components.QRScannerDialog
import app.mywifipass.model.data.QrData
import android.widget.Toast
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import app.mywifipass.backend.api_petitions.checkAttendee
import app.mywifipass.backend.api_petitions.authorizeAttendee
import kotlinx.coroutines.withContext


import androidx.compose.material.icons.filled.Check 
import androidx.compose.material.icons.filled.Close


@Composable 
fun AdminScreen(
    context: Context = LocalContext.current
){
    var showScannerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showValidatedUserDialog by remember { mutableStateOf(false) }
    var lastAuthorizeUrl by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    
    if (showScannerDialog) {
        QRScannerDialog(
            onDismiss = { 
                showScannerDialog = false 
            },
            onResult = { result ->
                scope.launch {
                    withContext(Dispatchers.IO){
                        try {
                            val qrData = Json.decodeFromString<QrData>(result)
                            val endpoint = qrData.validation_url
                            val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                            val token = sharedPreferences.getString("auth_token", null) ?: throw IllegalStateException("Auth token is missing")
                            checkAttendee(
                                endpoint = endpoint,
                                token = token,
                                onSuccess = { message, authorize_url -> 
                                    response = message
                                    failed = false
                                    lastAuthorizeUrl = authorize_url
                                    showValidatedUserDialog = true
                                },
                                onError = { error -> 
                                    response = error
                                    failed = true
                                    showValidatedUserDialog = true
                                }
                            )
                        } catch (e: Exception) {
                            response = "Error: ${e.message}"
                        }
                    }
                }
                showScannerDialog = false
            }
        )
    }

    if (showValidatedUserDialog) {
        AlertDialog(
            onDismissRequest = { showValidatedUserDialog = false },
            title = { Text("Scanned info:") },
            text = { 
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (failed) {
                            Icon(
                                Icons.Filled.Close, 
                                contentDescription = "Error", 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check, 
                                contentDescription = "Success", 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$response")
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if(!failed){
                            scope.launch {
                                withContext(Dispatchers.IO){
                                    try {
                                        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                                        val token = sharedPreferences.getString("auth_token", null) ?: throw IllegalStateException("Auth token is missing")
                                        authorizeAttendee(
                                            endpoint = lastAuthorizeUrl,
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
                        } 
                        showValidatedUserDialog = false

                    }
                ) {
                    Text("OK")
                }
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
            MyWifiPassTheme {
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
