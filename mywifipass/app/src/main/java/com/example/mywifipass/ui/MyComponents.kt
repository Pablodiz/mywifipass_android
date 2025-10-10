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
import androidx.compose.foundation.layout.statusBarsPadding
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
import app.mywifipass.backend.api_petitions.ApiResult
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay

// Imports for IconWithAttribution
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import app.mywifipass.R
import androidx.compose.ui.text.TextStyle

import app.mywifipass.ui.components.ShowText
import app.mywifipass.ui.components.NotificationHandler
import android.os.Build


// i18n
import androidx.compose.ui.res.stringResource

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
fun IconWithAttribution(
    @DrawableRes icon: Int,
    text: String,
    url: String,
    icon_size: Int,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(icon_size.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        val annotatedText = buildAnnotatedString {
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(style = androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(text)
            }
            pop()
        }

        ClickableText(
            text = annotatedText,
            style = TextStyle(
                fontSize = 12.sp,
                textAlign = TextAlign.Center 
            ),
            onClick = { offset: Int ->
                annotatedText.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            }
        )
    }
}

@Composable 
fun NoNetworksIcon(){
    IconWithAttribution(
        icon = R.drawable.no_networks_added,
        text = "Technology illustrations by Storyset",
        url = "https://storyset.com/technology",
        icon_size = 300
    )
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
            .statusBarsPadding()
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
    onQRResult: (String) -> Unit = {},
    showQrScanner: Boolean = false,
    onQRScannerDismiss: () -> Unit = {},
){
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
                if (networks.isEmpty()) {
                    // Show icon with attribution when there are no networks
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        NoNetworksIcon()
                        Text(
                            text = stringResource(R.string.no_wifi_passes_added_yet),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    MyCardList(
                        dataList = networks,
                        onItemClick = onNetworkClick
                    )
                }
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
                Text(stringResource(R.string.add_wifi_pass), modifier = Modifier.padding(start = 8.dp))
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
            onDismiss = onQRScannerDismiss,
            barcodeText = stringResource(R.string.user_qr_code)
        )
    } 
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
    var apiError by remember { mutableStateOf<app.mywifipass.backend.api_petitions.ApiResult?>(null) }
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
            // Convert Result error to ApiResult for consistent error handling
            val exception = result.exceptionOrNull()
            apiError = ApiResult(
                title = context.getString(R.string.network_error_title),
                message = exception?.message ?: context.getString(R.string.failed_to_load_networks),
                isSuccess = false,
                showTrace = true,
                fullTrace = "Load Networks Error: ${exception?.stackTraceToString()}"
            )
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
            
            val result = mainController.addNetworkFromUrlWithApiResult(initialWifiPassUrl)
            
            if (result.isSuccess == true) {
                // Recargar la lista de redes después de añadir una nueva
                val networksResult = mainController.getNetworks()
                if (networksResult.isSuccess) {
                    connections = networksResult.getOrNull() ?: emptyList()
                }
                ShowText.toastDirect(context, context.getString(R.string.network_added_successfully))
            } else {
                // Set error to show the ApiErrorDialog
                apiError = result
            }
            
            isLoading = false
        }
    }

    // Function to handle QR code scanning (parses QR to get URL)
    val handleQRResult: (String) -> Unit = { qrCodeText ->
        scope.launch {
            isLoading = true
            
            val result = mainController.addNetworkFromQRWithApiResult(qrCodeText)
            
            if (result.isSuccess == true) {
                refreshNetworks()
                ShowText.toastDirect(context, context.getString(R.string.network_added_successfully))
            } else {
                // Set error to show the ApiErrorDialog
                apiError = result
            }
            
            isLoading = false
        }
    }

    LaunchedEffect(apiError) {
        apiError?.let { errorResult: ApiResult ->
            ShowText.apiDialog(errorResult, onDismiss = { apiError = null })
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
        onQRResult = handleQRResult,        // For QR scanning
        showQrScanner = showQrScanner,
        onQRScannerDismiss = { showQrScanner = false },
    )
    
    // Add the NotificationHandler to show dialogs, toasts, etc.
    NotificationHandler(context = context)
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
        
        // Get strings in composable context
        val configureConnectionText = stringResource(R.string.configure_connection)
        val connectedText = stringResource(R.string.connected)
        val notConnectedYetText = stringResource(R.string.not_connected_yet)
        val deleteText = stringResource(R.string.delete)
        val deleteFailedText = stringResource(R.string.delete_failed)
        val connectionConfiguredSuccessfullyText = stringResource(R.string.connection_configured_successfully)
        val connectionFailedText = stringResource(R.string.connection_failed)

        val buttonState by remember(network.are_certificiates_decrypted, network.is_connection_configured) {
            mutableStateOf(
                when {
                    network.are_certificiates_decrypted && !network.is_connection_configured -> 
                        ButtonState(configureConnectionText, false)
                    network.is_connection_configured -> 
                        ButtonState(connectedText, true)
                    else -> 
                        ButtonState(notConnectedYetText, true)
                }
            )
        }

        LaunchedEffect(selectedNetworkId) {
            if (!network.is_connection_configured && !network.are_certificiates_decrypted){
                while (true) {
                    try {
                        val result = mainController.checkAuthorizedAndSendCSR(network)
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
                                    Text(deleteText, color = androidx.compose.ui.graphics.Color.Red)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    val result = mainController.deleteNetwork(network, wifiManager)
                                    if (result.isSuccess) {
                                        // ShowText.toastDirect(context, networkDeletedSuccessfullyText)
                                        // Go back after deleting the network
                                        onNavigateBack()
                                    } else {
                                        ShowText.toastDirect(context, result.exceptionOrNull()?.message ?: deleteFailedText)
                                    }
                                }
                            }
                            )
                        }
                    }
                }
            )

            if (!network.is_user_authorized){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                {   
                    Text(stringResource(R.string.show_qr_code_validator))
                    // QR Code section
                    QrCode(
                        data = QrInfo(network = network),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            } else {
                // Success message when network is authorized
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.wifi_network_configured_successfully),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Icons row: WiFi + Check
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
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
            
            if (network.is_connection_configured) {
                Text(
                    text = stringResource(R.string.having_problems_reconfigure),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            // Reconfigure the network
                            scope.launch {
                                val result = mainController.connectToNetwork(network, wifiManager)
                                if (result.isSuccess) {
                                    // Reload the network to get updated state
                                    val networks = mainController.getNetworks().getOrNull() ?: emptyList()
                                    currentNetwork = networks.find { it.id == selectedNetworkId }
                                } else {
                                    ShowText.toastDirect(context, result.exceptionOrNull()?.message ?: "Failed to reset configuration")
                                }
                            }
                        }
                )
            }
            
            // Action button for connecting/configuring network - always at bottom
            Button(
                enabled = !buttonState.isBlocked,
                onClick = {
                    if (!network.is_connection_configured && network.are_certificiates_decrypted) {
                        scope.launch {
                            val result = mainController.connectToNetwork(network, wifiManager)
                            if (result.isSuccess) {
                                // Only show success message in Android 10-
                                // As in 11+, the system has it's own way of notifying users
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                    ShowText.toastDirect(context, connectionConfiguredSuccessfullyText)
                                }
                                // Reload the network to get updated state
                                val networks = mainController.getNetworks().getOrNull() ?: emptyList()
                                currentNetwork = networks.find { it.id == selectedNetworkId }
                            } else {
                                ShowText.toastDirect(context, result.exceptionOrNull()?.message ?: connectionFailedText)
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