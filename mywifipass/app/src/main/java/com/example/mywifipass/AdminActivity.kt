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

import app.mywifipass.ui.components.TopBar
import app.mywifipass.ui.components.QRScannerDialog
import app.mywifipass.controller.AdminController
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


import androidx.compose.material.icons.filled.Check 
import androidx.compose.material.icons.filled.Close

import app.mywifipass.ui.components.ShowText
import app.mywifipass.ui.components.NotificationHandler

// i18n
import app.mywifipass.R
import androidx.compose.ui.res.stringResource

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
    var isLoading by remember { mutableStateOf(false) }
    
    // Initialize AdminController
    val adminController = remember { AdminController(context) }
    
    if (showScannerDialog) {
        QRScannerDialog(
            barcodeText = stringResource(R.string.validator_qr_code),
            onDismiss = { 
                showScannerDialog = false 
            },
            onResult = { result ->
                scope.launch {
                    isLoading = true
                    val validationResult = adminController.validateQR(result)
                    
                    if (validationResult.isSuccess) {
                        val attendeeResult = validationResult.getOrNull()!!
                        response = attendeeResult.message
                        failed = !attendeeResult.isSuccess
                        lastAuthorizeUrl = attendeeResult.authorizeUrl
                        showValidatedUserDialog = true
                    } else {
                        response = validationResult.exceptionOrNull()?.message ?: "Validation failed"
                        failed = true
                        showValidatedUserDialog = true
                    }
                    isLoading = false
                }
                showScannerDialog = false
            }
        )
    }

    if (showValidatedUserDialog) {
        AlertDialog(
            onDismissRequest = { showValidatedUserDialog = false },
            title = { Text(stringResource(R.string.scanned_info)) },
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
                                contentDescription = stringResource(R.string.error), 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                Icons.Filled.Check, 
                                contentDescription = stringResource(R.string.success), 
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
                        if(!failed && lastAuthorizeUrl.isNotEmpty()){
                            scope.launch {
                                val authResult = adminController.authorizeAttendee(lastAuthorizeUrl)
                                
                                if (authResult.isSuccess) {
                                    response = authResult.getOrNull() ?: "Authorization successful"
                                    
                                    ShowText.toast(response)
                                } else {
                                    val errorMsg = authResult.exceptionOrNull()?.message ?: "Authorization failed"
                                    ShowText.dialog("Authorization Failed", errorMsg)
                                }
                            }
                        } 
                        showValidatedUserDialog = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Add the notification handler to manage all notifications
    NotificationHandler(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.processing))
        } else {
            Button(
                onClick = { showScannerDialog = true },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.open_qr_scanner))
            }
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
                        TopBar(
                            title = stringResource(R.string.admin_panel),
                            onBackClick = { finish() },
                            actions = {
                                Box(){
                                    IconButton(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(40.dp)
                                            .align(Alignment.TopEnd), 
                                    onClick = { 
                                        // Handle logout action using AdminController
                                        val adminController = AdminController(context)
                                        adminController.logout()
                                        finish() 
                                    }
                                    ){
                                        Icon(Icons.Filled.Logout, contentDescription = "Logout")   
                                    }
                                }
                            }
                        )
                        AdminScreen(context)
                    }
                }
            }
        }
    }
}
