package com.example.get_eap_tls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme
import com.example.get_eap_tls.backend.api_petitions.*
import com.example.get_eap_tls.backend.database.*
import com.example.get_eap_tls.backend.httpPetition
import com.example.get_eap_tls.backend.wifi_connection.*
import com.example.get_eap_tls.backend.certificates.*

// Imports for the QR code scanner
import androidx.activity.compose.rememberLauncherForActivityResult

// Imports for the SpeedDial
import androidx.compose.runtime.saveable.rememberSaveable
import com.leinardi.android.speeddial.compose.*
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Link
import androidx.compose.animation.ExperimentalAnimationApi

// Imports for asking for permissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat

data class SpeedDialItem(
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AddEventButton(
    speedDialItems: List<SpeedDialItem>,
    content: @Composable (PaddingValues) -> Unit, 
) {
    var speedDialState by rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }

    Scaffold(
        floatingActionButton = {
            SpeedDial(
                state = speedDialState,
                onFabClick = { expanded ->
                    speedDialState = if (expanded) SpeedDialState.Collapsed else SpeedDialState.Expanded
                },
                fabClosedBackgroundColor = MaterialTheme.colorScheme.primary, 
                fabClosedContentColor = MaterialTheme.colorScheme.onPrimary, 
                fabOpenedBackgroundColor = MaterialTheme.colorScheme.secondary,
                fabOpenedContentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                speedDialItems.forEach { item ->
                    item {
                        ExtendedFloatingActionButton(
                            onClick = {
                                    speedDialState = SpeedDialState.Collapsed;
                                    item.onClick()     
                                    },
                            text = { Text(item.label) },
                            icon = { item.icon() }
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
fun MainScreen(){
    // Create scope for the coroutine (for async tasks)
    val scope = rememberCoroutineScope()
    // Get the context
    val context = LocalContext.current
    // Get the database
    val dataSource = DataSource(context)
    
    // Variables for the UI
    var reply by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var enteredText by remember { mutableStateOf("") }
    val wifiManager = context.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    var selectedNetwork by remember { mutableStateOf<Network?>(null)}
    var showNetworkDialog by remember { mutableStateOf(false) }
    var connections by remember { mutableStateOf<List<Network>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    val cameraPermission = android.Manifest.permission.CAMERA
    var hasCameraPermission by remember { mutableStateOf(false) }

    // Check if the camera permission is granted
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) {
                // If its conceeded, open the QRDialog
                showQrScanner = true
            } else {
                // Else, show a message for the user to know that the permission is required
                Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
                showQrScanner = false
            }
        }
)

    // Get data from the database
    LaunchedEffect(Unit) {
        connections = withContext(Dispatchers.IO) {
            dataSource.loadConnections()
        }
    }

    AddEventButton(
        speedDialItems = listOf(
            SpeedDialItem(
                label = "Scan QR",
                icon = { Icon(Icons.Filled.QrCode, contentDescription="Scan QR" ) },
                onClick = { showQrScanner = true }
            ),
            SpeedDialItem(
                label = "Enter URL",
                icon = { Icon(Icons.Filled.Link, contentDescription="Enter URL") },
                onClick = { showDialog = true}
            )
        ),
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                MyCardList(
                    dataList = connections,
                    onItemClick = { network ->
                        selectedNetwork = network
                        showNetworkDialog = true
                    }
                )
            }
        }
    )   


    val handlePetition: (String) -> Unit = { newText ->
        // In a secondary thread, we make the petition and add the network to the database
        scope.launch {
            connections = withContext(Dispatchers.IO) {
                makePetitionAndAddToDatabase(
                    enteredText = newText,
                    dataSource = dataSource,
                    onSuccess = { reply = it },
                    onError = { reply = it }
                )
            }
        }
    }
    
    if (showQrScanner) {
        hasCameraPermission = context.checkSelfPermission(cameraPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            // If the permission was denied before, show the app configuration for the user to enable it 
            if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, cameraPermission)) {
                // Request the permission
                permissionLauncher.launch(cameraPermission)
            } else {
                // Redirect to app settings
                Toast.makeText(context, "Camera permission is required. Please enable it in settings.", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                showQrScanner = false
            }
        } else {
            // Mostrar el QRScannerDialog si ya tiene permisos
            QRScannerDialog(
                onResult = { scannedText ->
                    handlePetition(scannedText)
                    showQrScanner = false
                },
                onDismiss = { 
                    showQrScanner = false 
                }
            )
        }
    }

    // Dialog for adding a new network entering an URL
    MyDialog(
        showDialog = showDialog, 
        onDismiss = { 
            showDialog = false
        }, 
        onAccept = { 
            showDialog = false
            handlePetition(enteredText)          
        },
        content = {
            Column{
                MyTextField(  
                    onTextChange = { newText ->
                        enteredText = newText 
                    },
                    label = "Enter URL",
                    placeholder = "https://example.com"
                )
            }
        },
        dialogTitle = "Add new network",
    )   

    // Dialog for showing the information of the selected network
    if (showNetworkDialog && selectedNetwork != null) {
        NetworkDialog(
            showDialog = showNetworkDialog,
            onDismiss = {
                showNetworkDialog = false
                selectedNetwork = null
            },
            onAccept = { network ->
                showNetworkDialog = false    
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dataSource.updateNetwork(network)   
                        connections = dataSource.loadConnections()
                    }
                }
                selectedNetwork = null                 
            },
            selectedNetwork = selectedNetwork!!,
            wifiManager = wifiManager,
            dataSource = dataSource,
            connections = connections,
            scope = scope,
            onConnectionsUpdated = { updatedConnections ->
                connections = updatedConnections
            }, 
            showToast = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
