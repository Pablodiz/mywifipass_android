package app.mywifipass.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import app.mywifipass.model.data.Network
import app.mywifipass.controller.MainController

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
fun MainScreen(
    modifier: Modifier = Modifier,
    networks: List<Network> = emptyList(),
    isLoading: Boolean = false,
    onNetworkClick: (Network) -> Unit = {},
    onScanQRClick: () -> Unit = {},
    onEnterURLClick: () -> Unit = {},
    onQRResult: (String) -> Unit = {},
    onURLEntered: (String) -> Unit = {},
    showQrScanner: Boolean = false,
    onQRScannerDismiss: () -> Unit = {},
    showUrlDialog: Boolean = false,
    onUrlDialogDismiss: () -> Unit = {},
    onUrlDialogAccept: (String) -> Unit = {}
){
    // Variables for the URL dialog
    var enteredText by remember { mutableStateOf("") }

    AddEventButton(
        speedDialItems = listOf(
            SpeedDialItem(
                label = "Scan QR",
                icon = { Icon(Icons.Filled.QrCode, contentDescription="Scan QR" ) },
                onClick = onScanQRClick
            ),
            SpeedDialItem(
                label = "Enter URL",
                icon = { Icon(Icons.Filled.Link, contentDescription="Enter URL") },
                onClick = onEnterURLClick
            )
        ),
        content = { paddingValues ->
            Column(modifier = modifier.padding(paddingValues)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    MyCardList(
                        dataList = networks,
                        onItemClick = onNetworkClick
                    )
                }
            }
        }
    )   

    if (showQrScanner) {
        QRScannerDialog(
            onResult = { scannedText ->
                onQRResult(scannedText)
                onQRScannerDismiss()
            },
            onDismiss = onQRScannerDismiss
        )
    }

    // Dialog for adding a new network entering an URL
    MyDialog(
        showDialog = showUrlDialog, 
        onDismiss = onUrlDialogDismiss, 
        onAccept = { 
            onUrlDialogAccept(enteredText)
            enteredText = ""
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
}

// MainScreen Container that handles business logic
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(modifier: Modifier = Modifier){
    // Create scope for the coroutine (for async tasks)
    val scope = rememberCoroutineScope()
    // Get the context
    val context = LocalContext.current
    // Initialize MainController
    val mainController = remember { MainController(context) }
    
    // Variables for the UI state
    var error by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    val wifiManager = context.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    var selectedNetwork by remember { mutableStateOf<Network?>(null)}
    var showNetworkDialog by remember { mutableStateOf(false) }
    var connections by remember { mutableStateOf<List<Network>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Load networks from controller
    LaunchedEffect(Unit) {
        isLoading = true
        val result = mainController.getNetworks()
        if (result.isSuccess) {
            connections = result.getOrNull() ?: emptyList()
        } else {
            error = result.exceptionOrNull()?.message ?: "Failed to load networks"
        }
        isLoading = false
    }

    // Function to refresh networks list
    val refreshNetworks : () -> Unit ={
        scope.launch {
            val networksResult = mainController.getNetworks()
            if (networksResult.isSuccess) {
                connections = networksResult.getOrNull() ?: emptyList()
            }
        }
    }

    // Function to handle QR code scanning (parses QR to get URL)
    val handleQRResult: (String) -> Unit = { qrCodeText ->
        scope.launch {
            isLoading = true
            val result = mainController.addNetworkFromQR(qrCodeText) // This parses QR and extracts URL
            
            if (result.isSuccess) {
                refreshNetworks()
                Toast.makeText(context, "Network added successfully from QR", Toast.LENGTH_SHORT).show()
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to add network from QR"
            }
            isLoading = false
        }
    }

    // Function to handle direct URL input
    val handleURLInput: (String) -> Unit = { url ->
        scope.launch {
            isLoading = true
            val result = mainController.addNetworkFromUrl(url) // This uses URL directly
            
            if (result.isSuccess) {
                refreshNetworks()
                Toast.makeText(context, "Network added successfully from URL", Toast.LENGTH_SHORT).show()
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to add network from URL"
            }
            isLoading = false
        }
    }
    
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            error = ""
        }
    }

    // Pure UI Component
    MainScreen(
        modifier = modifier,
        networks = connections,
        isLoading = isLoading,
        onNetworkClick = { network ->
            selectedNetwork = network
            showNetworkDialog = true
        },
        onScanQRClick = { showQrScanner = true },
        onEnterURLClick = { showUrlDialog = true },
        onQRResult = handleQRResult,        // For QR scanning
        onURLEntered = handleURLInput,      // For direct URL input (not used anymore)
        showQrScanner = showQrScanner,
        onQRScannerDismiss = { showQrScanner = false },
        showUrlDialog = showUrlDialog,
        onUrlDialogDismiss = { showUrlDialog = false },
        onUrlDialogAccept = { url ->
            showUrlDialog = false
            handleURLInput(url)             // Use URL input function for dialog
        }
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
            selectedNetworkId = selectedNetwork!!.id,
            wifiManager = wifiManager,
            mainController = mainController,
            scope = scope,
            onConnectionsUpdated = refreshNetworks,
            showToast = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}