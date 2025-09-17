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
import androidx.compose.foundation.background
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
import app.mywifipass.NetworkDetailActivity

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

// Imports for Network Detail Screen
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay

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

@Composable
fun TopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .height(56.dp)
    ) {
        // Apply color and text styles
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onPrimary
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {

                // Left side - Back button
                if (onBackClick != null) {
                    BackButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                // Center - Title
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Right side - Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    actions()
                }
            }
        }
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
    // var enteredText by remember { mutableStateOf("") }

    // #DEPRECATED 
    //AddEventButton(
    //     speedDialItems = listOf(
    //         SpeedDialItem(
    //             label = "Scan QR",
    //             icon = { Icon(Icons.Filled.QrCode, contentDescription="Scan QR" ) },
    //             onClick = onScanQRClick
    //         ),
    //         SpeedDialItem(
    //             label = "Enter URL",
    //             icon = { Icon(Icons.Filled.Link, contentDescription="Enter URL") },
    //             onClick = onEnterURLClick
    //         )
    //     ),
    //     content = { paddingValues ->
    //         Column(modifier = modifier.padding(paddingValues)) {
    //             if (isLoading) {
    //                 Box(
    //                     modifier = Modifier.fillMaxSize(),
    //                     contentAlignment = Alignment.Center
    //                 ) {
    //                     CircularProgressIndicator()
    //                 }
    //             } else {
    //                 MyCardList(
    //                     dataList = networks,
    //                     onItemClick = onNetworkClick
    //                 )
    //             }
    //         }
    //     }
    // )   
    
    
    // Main layout with button at bottom
    Column(modifier = modifier.fillMaxSize()) {
        // Networks list takes all available space
        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                MyCardList(
                    dataList = networks,
                    onItemClick = onNetworkClick
                )
            }
        }
        
        // Button for adding events via a QR Code - always at bottom
        Button(
            onClick = onScanQRClick,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.QrCode, contentDescription="Add Wifi Pass")
                Text("Add a new Wifi Pass", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    // Dialog for adding a new network scanning a QR Code
    if (showQrScanner) {
        QRScannerDialog(
            onResult = { scannedText ->
                onQRResult(scannedText)
                onQRScannerDismiss()
            },
            onDismiss = onQRScannerDismiss
        )
    }

    // #DEPRECATED
    // Dialog for adding a new network entering an URL
    // MyDialog(
    //     showDialog = showUrlDialog, 
    //     onDismiss = onUrlDialogDismiss, 
    //     onAccept = { 
    //         onUrlDialogAccept(enteredText)
    //         enteredText = ""
    //     },
    //     content = {
    //         Column{
    //             TextField(
    //                 value = enteredText,
    //                 onValueChange = { enteredText = it },
    //                 label = {Text("Enter URL")},
    //                 placeholder = {Text("https://example.com")}
    //             )
    //         }
    //     },
    //     dialogTitle = "Add new network",
    // )   
}

// MainScreen Container that handles business logic
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(modifier: Modifier = Modifier, initialWifiPassUrl: String? = null){
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
    var connections by remember { mutableStateOf<List<Network>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Function to refresh networks list - define it first
    val refreshNetworks : () -> Unit = {
        scope.launch {
            val networksResult = mainController.getNetworks()
            if (networksResult.isSuccess) {
                connections = networksResult.getOrNull() ?: emptyList()
            }
        }
    }

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

    // Listen for lifecycle changes to refresh when returning from other activities
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh networks when returning to this screen
                scope.launch {
                    refreshNetworks()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Procesar automáticamente la URL del deep link si existe
    LaunchedEffect(initialWifiPassUrl) {
        if (!initialWifiPassUrl.isNullOrEmpty()) {
            isLoading = true
            scope.launch {
                val result = mainController.addNetworkFromUrl(initialWifiPassUrl)
                if (result.isSuccess) {
                    // Recargar la lista de redes después de añadir una nueva
                    val networksResult = mainController.getNetworks()
                    if (networksResult.isSuccess) {
                        connections = networksResult.getOrNull() ?: emptyList()
                    }
                } else {
                    error = result.exceptionOrNull()?.message ?: "Failed to add network from URL"
                }
                isLoading = false
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
                // Toast.makeText(context, "Network added successfully from QR", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, "Network added successfully", Toast.LENGTH_SHORT).show()
            } else {
                // error = result.exceptionOrNull()?.message ?: "Failed to add network"
                error = result.exceptionOrNull()?.message ?: "Failed to add network"
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
                // Toast.makeText(context, "Network added successfully from URL", Toast.LENGTH_SHORT).show()
                Toast.makeText(context, "Network added successfully", Toast.LENGTH_SHORT).show()
            } else {
                // error = result.exceptionOrNull()?.message ?: "Failed to add network from URL"
                error = result.exceptionOrNull()?.message ?: "Failed to add network"
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
            // Navigate to NetworkDetailActivity instead of showing dialog
            NetworkDetailActivity.start(context, network.id)
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
}


data class ButtonState(val text: String, val isBlocked: Boolean)

@Composable
fun NetworkDetailScreen(
    modifier:Modifier = Modifier, 
    selectedNetworkId: Int,
    wifiManager: android.net.wifi.WifiManager,
    mainController: MainController,
    onNavigateBack: () -> Unit = {}
){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentNetwork by remember {mutableStateOf<Network?>(null)}

    // Load initial network when this screen opens
    LaunchedEffect(selectedNetworkId){
        val networks = mainController.getNetworks().getOrNull()?:emptyList()
        currentNetwork = networks.find {it.id == selectedNetworkId}
    }

    currentNetwork?.let {network ->
        var menuExpanded by remember { mutableStateOf(false) }

        val buttonState by remember(network.are_certificiates_decrypted, network.is_connection_configured) {
            mutableStateOf(
                when {
                    network.are_certificiates_decrypted && !network.is_connection_configured -> 
                        ButtonState("Configure connection", false)
                    network.is_connection_configured -> 
                        ButtonState("Connected", true)
                    else -> 
                        ButtonState("Not connected yet", true)
                }
            )
        }

        LaunchedEffect(selectedNetworkId) {
            if (!network.is_connection_configured && !network.are_certificiates_decrypted){
                while (true) {
                    try {
                        val result = mainController.downloadCertificates(network)
                        if (result.isSuccess) {
                            val networks = mainController.getNetworks().getOrNull() ?: emptyList()
                            currentNetwork = networks.find { it.id == selectedNetworkId }
                            break
                        } else {
                            throw result.exceptionOrNull() ?: Exception("Failed to download certificates")
                        }
                    } catch (e: Exception) {
                        // Continue trying
                        // Toast.makeText(
                        //     context,
                        //     "${e.message}",
                        //     Toast.LENGTH_SHORT
                        // ).show()
                    }
                    delay(10_000L) // Wait 10 seconds before trying again
                }
            }
        }

        Column(modifier=modifier){
            // Top bar with back button, title and menu
            TopBar(
                title = network.location_name,
                onBackClick = onNavigateBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete", color = androidx.compose.ui.graphics.Color.Red)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    val result = mainController.deleteNetwork(network, wifiManager)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Network deleted successfully", Toast.LENGTH_SHORT).show()
                                        // Go back after deleting the network
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            )
                        }
                    }
                }
            )

            // QR Code section
            QrCode(
                data = QrInfo(network = network),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Network information section - scrollable content
            Box(
                modifier = Modifier
                    .weight(1f) // Take all available space
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    NetworkDialogEventInfo(network = network)
                }
            }
            
            // Action button for connecting/configuring network - always at bottom
            Button(
                enabled = !buttonState.isBlocked,
                onClick = {
                    if (!network.is_connection_configured && network.are_certificiates_decrypted) {
                        scope.launch {
                            val result = mainController.connectToNetwork(network, wifiManager)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Connection configured successfully", Toast.LENGTH_SHORT).show()
                                // Reload the network to get updated state
                                val networks = mainController.getNetworks().getOrNull() ?: emptyList()
                                currentNetwork = networks.find { it.id == selectedNetworkId }
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Connection failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                ) {
                Text(buttonState.text)
            }
        }
    }
}