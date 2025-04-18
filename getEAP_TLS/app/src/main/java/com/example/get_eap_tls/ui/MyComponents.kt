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
fun NetworkDialogInfo(network: Network) {
    Column {
        InfoText("User Name", network.user_name)
        InfoText("User Email", network.user_email)
        InfoText("User ID Document", network.user_id_document)
        InfoText("Event's name", network.location_name)
        InfoText("Location", network.location)
        InfoText("Start date", network.start_date)
        InfoText("End date", network.end_date)
        InfoText("Description", network.description)
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
        QRScannerDialog(
            onResult = { scannedText ->
                handlePetition(scannedText)
                showQrScanner = false
            },
            onDismiss = { showQrScanner = false }
        )
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
        MyDialog(
            showDialog = showNetworkDialog, 
            onDismiss = { 
                showDialog = false
                selectedNetwork = null
            }, 
            onAccept = { 
                showDialog = false    
                selectedNetwork = null                             
            },
            content = {
                Column{
                    NetworkDialogInfo(network = selectedNetwork!!)
                    // Button that connects to the network
                    Button(
                        onClick = {
                            scope.launch {
                                val certificates = EapTLSCertificate(
                                    selectedNetwork!!.ca_certificate.byteInputStream(),
                                    selectedNetwork!!.certificate.byteInputStream(),
                                    selectedNetwork!!.private_key.byteInputStream()
                                )
                                val eapTLSConnection = EapTLSConnection(
                                    selectedNetwork!!.ssid, 
                                    certificates, 
                                    selectedNetwork!!.user_email, //The identity musn't have blank spaces 
                                    selectedNetwork!!.network_common_name
                                ) 
                                eapTLSConnection.connect(wifiManager)                
                            }
                        }
                    ) {
                        Text("Connect")
                    }
                    // Button that the deletes this from the database
                    Button(
                        onClick = {
                            val networkToDelete = selectedNetwork
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    dataSource.deleteNetwork(networkToDelete!!)
                                    connections = dataSource.loadConnections()
                                }
                            }
                            showDialog = false
                            selectedNetwork = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            },
            dialogTitle = "${selectedNetwork!!.location_name}",
        )      
    }
}
