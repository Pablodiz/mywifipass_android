/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

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
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network as AndroidNetwork
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import app.mywifipass.backend.isConnectedToWifi
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

// Imports for the SpeedDial
import androidx.compose.runtime.saveable.rememberSaveable
import com.leinardi.android.speeddial.compose.*
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Link
import androidx.compose.animation.ExperimentalAnimationApi

// Imports for Back Button
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff

// Imports for Network Detail Screen
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
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

@Composable
fun rememberWifiEnabled(): Boolean {
    val context = LocalContext.current
    val wifiManager = remember {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    var isEnabled by remember { mutableStateOf(wifiManager.isWifiEnabled) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                isEnabled = state == WifiManager.WIFI_STATE_ENABLED
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        onDispose { context.unregisterReceiver(receiver) }
    }

    return isEnabled
}

@Composable
fun WifiDisabledBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.wifi_disabled_banner),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ConnectionStatusSection(ssid: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var onWifi by remember { mutableStateOf(isConnectedToWifi(context)) }

    DisposableEffect(Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: AndroidNetwork) { onWifi = isConnectedToWifi(context) }
            override fun onLost(network: AndroidNetwork) { onWifi = isConnectedToWifi(context) }
            override fun onCapabilitiesChanged(network: AndroidNetwork, caps: NetworkCapabilities) {
                onWifi = isConnectedToWifi(context)
            }
        }
        try { connectivityManager.registerDefaultNetworkCallback(callback) } catch (_: Exception) {}
        onDispose {
            try { connectivityManager.unregisterNetworkCallback(callback) } catch (_: Exception) {}
        }
    }

    val (text, color) = if (onWifi) {
        Pair(stringResource(R.string.connection_wifi_unreadable_ssid, ssid), MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Pair(stringResource(R.string.connection_not_on_wifi), MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

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
    onNetworkLongClick: (Network) -> Unit = {},
    onScanQRClick: () -> Unit = {},
    onQRResult: (String) -> Unit = {},
    showQrScanner: Boolean = false,
    onQRScannerDismiss: () -> Unit = {},
){
    val isWifiEnabled = rememberWifiEnabled()

    // Main layout with button at bottom
    Column(modifier = modifier.fillMaxSize()) {
        if (!isWifiEnabled) {
            WifiDisabledBanner()
        }
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
                        onItemClick = onNetworkClick,
                        onItemLongClick = onNetworkLongClick,
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
    val fido2ValidationSuccessfulText = stringResource(R.string.fido2_validation_successful)
    val fido2ValidationFailedText = stringResource(R.string.fido2_validation_failed)
    var connections by remember { mutableStateOf<List<Network>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var networkToDelete by remember { mutableStateOf<Network?>(null) }

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
            
            val result = mainController.addNetworkFromUrlWithApiResult(initialWifiPassUrl, wifiManager)
            
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
            
            val result = mainController.addNetworkFromQRWithApiResult(qrCodeText, wifiManager)
            
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
            scope.launch {
                if (network.requires_fido2_validation && !network.is_user_authorized) {
                    val result = mainController.validateWithFido2(network, context)
                    if (result.isFailure) {
                        ShowText.toastDirect(
                            context,
                            result.exceptionOrNull()?.message ?: fido2ValidationFailedText
                        )
                        return@launch
                    }

                    ShowText.toastDirect(context, fido2ValidationSuccessfulText)
                    refreshNetworks()
                }

                // Navigate to detail after list-level FIDO2 gate.
                NetworkDetailActivity.start(context, network.id)
            }
        },
        onNetworkLongClick = { network -> networkToDelete = network },
        onScanQRClick = { showQrScanner = true },
        onQRResult = handleQRResult,
        showQrScanner = showQrScanner,
        onQRScannerDismiss = { showQrScanner = false },
    )

    networkToDelete?.let { network ->
        val deleteTitle = stringResource(R.string.delete_pass_title)
        val deleteMessage = stringResource(R.string.delete_pass_message, network.location_name)
        val deleteLabel = stringResource(R.string.delete)
        val cancelLabel = stringResource(R.string.cancel)
        val deletedText = stringResource(R.string.network_deleted_successfully)
        val deleteFailedText = stringResource(R.string.delete_failed)

        AlertDialog(
            onDismissRequest = { networkToDelete = null },
            title = { Text(deleteTitle) },
            text = { Text(deleteMessage) },
            confirmButton = {
                TextButton(onClick = {
                    val target = network
                    networkToDelete = null
                    scope.launch {
                        val result = mainController.deleteNetwork(target, wifiManager)
                        if (result.isSuccess) {
                            refreshNetworks()
                            ShowText.toastDirect(context, deletedText)
                        } else {
                            ShowText.toastDirect(
                                context,
                                result.exceptionOrNull()?.message ?: deleteFailedText
                            )
                        }
                    }
                }) { Text(deleteLabel) }
            },
            dismissButton = {
                TextButton(onClick = { networkToDelete = null }) { Text(cancelLabel) }
            }
        )
    }

    // Add the NotificationHandler to show dialogs, toasts, etc.
    NotificationHandler(context = context)
}


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
    var currentNetwork by remember { mutableStateOf<Network?>(null) }

    var awaitingSystemDialog by remember { mutableStateOf(false) }
    var systemDialogDenied by remember { mutableStateOf(false) }

    val addNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        awaitingSystemDialog = false
        if (result.resultCode == Activity.RESULT_OK) {
            systemDialogDenied = false
            scope.launch {
                currentNetwork?.let { net ->
                    mainController.markNetworkConnected(net).getOrNull()?.let { updated ->
                        currentNetwork = updated
                    }
                }
            }
        } else {
            systemDialogDenied = true
        }
    }

    // Load initial network when this screen opens
    LaunchedEffect(selectedNetworkId){
        val networks = mainController.getNetworks().getOrNull() ?: emptyList()
        currentNetwork = networks.find { it.id == selectedNetworkId }
    }

    currentNetwork?.let {network ->
        var menuExpanded by remember { mutableStateOf(false) }
        
        // Get strings in composable context
        val deleteText = stringResource(R.string.delete)
        val deleteFailedText = stringResource(R.string.delete_failed)
        val connectionConfiguredSuccessfullyText = stringResource(R.string.connection_configured_successfully)
        val connectionFailedText = stringResource(R.string.connection_failed)
        val needsFido2Validation = network.requires_fido2_validation && !network.is_user_authorized

        LaunchedEffect(selectedNetworkId) {
            if (!network.is_connection_configured &&
                !network.are_certificiates_decrypted &&
                !needsFido2Validation
            ){
                while (true) {
                    try {
                        val result = mainController.checkAuthorizedAndConnect(network, wifiManager)
                        if (result.isSuccess) {
                            val networks = mainController.getNetworks().getOrNull() ?: emptyList()
                            currentNetwork = networks.find { it.id == selectedNetworkId }
                            break
                        } else {
                            throw result.exceptionOrNull() ?: Exception("Failed to authorize and configure connection")
                        }
                    } catch (e: Exception) {
                        // Continue trying
                    }
                    delay(5_000L) // Wait 5 seconds before trying again
                }
            }
        }

        // Auto-configure once certificates are available.
        LaunchedEffect(network.are_certificiates_decrypted, network.is_connection_configured) {
            if (network.are_certificiates_decrypted && !network.is_connection_configured && !awaitingSystemDialog) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: launch system dialog via launcher, mark configured only on RESULT_OK
                    val intentResult = mainController.buildConnectionIntent(network)
                    if (intentResult.isSuccess) {
                        awaitingSystemDialog = true
                        systemDialogDenied = false
                        addNetworkLauncher.launch(intentResult.getOrNull()!!)
                    } else {
                        ShowText.toastDirect(context, intentResult.exceptionOrNull()?.message ?: connectionFailedText)
                    }
                } else {
                    // Android 10-: suggestion API, result is immediate
                    var attempts = 0
                    val maxAttempts = 6
                    while (attempts < maxAttempts) {
                        val result = mainController.connectToNetwork(network, wifiManager)
                        if (result.isSuccess) {
                            ShowText.toastDirect(context, connectionConfiguredSuccessfullyText)
                            currentNetwork = result.getOrNull()
                            break
                        }
                        attempts += 1
                        if (attempts >= maxAttempts) {
                            ShowText.toastDirect(context, result.exceptionOrNull()?.message ?: connectionFailedText)
                            break
                        }
                        delay(2_000L)
                    }
                }
            }
        }

        val isWifiEnabled = rememberWifiEnabled()

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

            if (!isWifiEnabled) {
                WifiDisabledBanner()
            }

            if (awaitingSystemDialog) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.wifi_configuration_pending),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (systemDialogDenied) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.wifi_configuration_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        val intentResult = mainController.buildConnectionIntent(network)
                        if (intentResult.isSuccess) {
                            systemDialogDenied = false
                            awaitingSystemDialog = true
                            addNetworkLauncher.launch(intentResult.getOrNull()!!)
                        }
                    }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else if (!network.is_user_authorized && !network.requires_fido2_validation){
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
            } else if (network.requires_fido2_validation && !network.is_user_authorized) {
                // FIDO2-required networks should not show "configured" before authorization.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.fido2_validation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (network.is_connection_configured) {
                // Only show success when the device is actually configured (dialog accepted).
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

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    ConnectionStatusSection(ssid = network.ssid)
                }
            } else if (network.is_user_authorized) {
                // Authorized by server but device not yet configured — show spinner while we work.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.wifi_configuration_pending),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
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
                        .navigationBarsPadding()
                        .padding(16.dp)
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


            
            // FIDO2 validation is now launched directly when entering this screen.
            
            // Action button for connecting/configuring network - always at bottom
            // DEPRECATED, now the configuration is done automatically
            // Button(
            //     enabled = !buttonState.isBlocked,
            //     onClick = {
            //         if (!network.is_connection_configured && network.are_certificiates_decrypted) {
            //             scope.launch {
            //                 val result = mainController.connectToNetwork(network, wifiManager)
            //                 if (result.isSuccess) {
            //                     // Only show success message in Android 10-
            //                     // As in 11+, the system has it's own way of notifying users
            //                     if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            //                         ShowText.toastDirect(context, connectionConfiguredSuccessfullyText)
            //                     }
            //                     // Reload the network to get updated state
            //                     val networks = mainController.getNetworks().getOrNull() ?: emptyList()
            //                     currentNetwork = networks.find { it.id == selectedNetworkId }
            //                 } else {
            //                     ShowText.toastDirect(context, result.exceptionOrNull()?.message ?: connectionFailedText)
            //                 }
            //             }
            //         }
            //     },
            //     modifier = Modifier
            //          .fillMaxWidth()
            //          .navigationBarsPadding()
            //          .padding(16.dp)
            //     ) {
            //     Text(buttonState.text)
            // }
        }
    }
}