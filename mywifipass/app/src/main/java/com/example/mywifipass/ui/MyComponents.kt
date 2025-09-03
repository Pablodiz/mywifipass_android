package app.mywifipass.ui.components

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

import app.mywifipass.ui.theme.MyWifiPassTheme
import app.mywifipass.backend.api_petitions.*
import app.mywifipass.backend.database.*
import app.mywifipass.model.data.Network
import app.mywifipass.backend.httpPetition
import app.mywifipass.backend.wifi_connection.*
import app.mywifipass.backend.certificates.*

// Imports for the QR code scanner
import androidx.activity.compose.rememberLauncherForActivityResult

// Imports for the SpeedDial
import androidx.compose.runtime.saveable.rememberSaveable
import com.leinardi.android.speeddial.compose.*
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Link
import androidx.compose.animation.ExperimentalAnimationApi



// Imports for Back Button
import androidx.compose.material.icons.filled.ArrowBack

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
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick, 
        modifier = modifier 
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
    
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier){
    // Create scope for the coroutine (for async tasks)
    val scope = rememberCoroutineScope()
    // Get the context
    val context = LocalContext.current
    // Get the database
    val dataSource = DataSource(context)
    
    // Variables for the UI
    var reply by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
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
            Column(modifier = modifier.padding(paddingValues)) {
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
                    onSuccess = { 
                        reply = it                
                    },
                    onError = { 
                        error = it 
                    }
                )
            }
        }
    }
    
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            error = ""
        }
    }

    if (showQrScanner) {
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
                TextField(
                    value = enteredText,
                    onValueChange = { enteredText = it },
                    label = {Text("Enter URL")},
                    placeholder = {Text("https://example.com")}
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
            onAccept = {
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
