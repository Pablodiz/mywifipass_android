package com.example.get_eap_tls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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

import com.example.get_eap_tls.backend.api_petitions.parseReply
import com.example.get_eap_tls.backend.database.Network
import com.example.get_eap_tls.backend.database.DataSource
import com.example.get_eap_tls.backend.peticionHTTP
import com.example.get_eap_tls.backend.wifi_connection.EapTLSConnection
import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import android.content.Intent
import com.google.zxing.ResultPoint
import com.google.zxing.BarcodeFormat

@Composable
// Funcion que muestra un dialogo emergente
fun MyDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit, // Función que se ejecuta al cancelar, normalemente cerrar la ventana
    onAccept: () -> Unit, 
    dialogTitle: String = "Dialog's title",
    dialogContent: String = "This is the dialog's content",
    acceptButtonText: String = "Accept",
    cancelButtonText: String = "Cancel",
    content: @Composable () -> Unit = {Text(dialogContent)} // Utilizamos una función para poder pasarle un composable y cambiar el content
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(dialogTitle) },
            text = { content() },
            confirmButton = {
                TextButton(onClick = {
                    onAccept()
                    onDismiss()
                }) {
                    Text(acceptButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDismiss() 
                }) {
                    Text(cancelButtonText)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTextField(
    label: String = "Write your text here:",
    placeholder: String = "Your text...",
    onTextChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { 
            text = it 
            onTextChange(it)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) }
    )
    
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventButton(
    onFabClick : () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {onFabClick()}
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add event"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ){ paddingValues -> 
        content(paddingValues)
    }
}

@Composable
fun InfoText(label: String, value: String) {
    if (value.isNotEmpty()) {
        Text("$label: $value")
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
fun NetworkCardInfo(network: Network) {
    Column {
        InfoText("Location", network.location)
        InfoText("Start date", network.start_date)
        InfoText("End date", network.end_date)
    }
}

@Composable
fun QRScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            AndroidView(
                factory = { context ->
                    // Create the barcode scanner view
                    val barcodeView = DecoratedBarcodeView(context)
                    // Set the decoder factory to only recognize QR codes
                    barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                    // Set the help text
                    barcodeView.setStatusText("Scan a QR code")
                    // Start the camera decode (the user doesnt have to click another button)                
                    barcodeView.decodeSingle(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult?) {
                            result?.text?.let {
                                onResult(it)
                                onDismiss()
                            }
                        }
                        // List of the points that are analyzed by the scanner
                        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
                    })
                    barcodeView.resume()
                    barcodeView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}

suspend fun performPetitionAndUpdateDatabase(
    context: Context,
    enteredText: String,
    dataSource: DataSource
): List<Network> {
    return withContext(Dispatchers.IO) {
        try {
            val reply = peticionHTTP(enteredText)
            val network = parseReply(reply)
            dataSource.insertNetwork(network)
            dataSource.loadConnections() 
        } catch (e: Exception) {
            Log.e("performPetition", e.message ?: "Unknown error")
            dataSource.loadConnections() 
        }
    }
}

suspend fun makePetitionAndAddToDatabase(
    context: Context,
    enteredText: String,
    dataSource: DataSource, 
    onSuccess: (String) -> Unit = {},
    onError: (String) -> Unit = {}
):List<Network> {
    withContext(Dispatchers.IO) {
        try{
            val reply = peticionHTTP(enteredText)
            val network = parseReply(reply)
            dataSource.insertNetwork(network)
            onSuccess(reply) 
        } catch (e:Exception){
            onError(e.message.toString())
        }
    }
    return dataSource.loadConnections()
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
        onFabClick = { showDialog = true },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Button(onClick = { showQrScanner = true }) {
                    Text("Scan QR")
                }
                //Text(reply)
                MyCardList(dataList = connections 
                ,onItemClick = { network -> 
                    selectedNetwork = network
                    showNetworkDialog = true       
                    }
                )                
            }
        }
    )
    
    if (showQrScanner) {
        QRScannerDialog(
            onResult = { enteredText ->
                scope.launch{
                    connections = withContext(Dispatchers.IO) {
                        makePetitionAndAddToDatabase(
                            context = context, 
                            enteredText = enteredText, 
                            dataSource = dataSource,
                            onSuccess = { reply = it },
                            onError = { reply = it }
                        )
                    }
                }  
                showQrScanner = false
            },
            onDismiss = { showQrScanner = false }
        )
    }
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

    // Dialog for adding a new network
    MyDialog(
        showDialog = showDialog, 
        onDismiss = { 
            showDialog = false
        }, 
        onAccept = { 
            showDialog = false 
            // En un hilo secundario, hacer la petición HTTP y mostrar un mensaje de que está ocurriendo
            scope.launch{
                scope.launch{
                    connections = withContext(Dispatchers.IO) {
                        makePetitionAndAddToDatabase(
                            context = context, 
                            enteredText = enteredText, 
                            dataSource = dataSource,
                            onSuccess = { reply = it },
                            onError = { reply = it }
                        )
                    }
                }   
            }
        },
        content = {
            Column{
                MyTextField( 
                    onTextChange = { enteredText = it },
                    label = "Enter URL",
                    placeholder = "https://example.com"
                )
            }
        },
        dialogTitle = "Add new network",
    )   
}

@Composable
fun MyCard(
    data: Network,
    onItemClick: (Network) -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick(data) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = data.location_name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            NetworkCardInfo(network = data)
        }
    }
}


@Composable
fun MyCardList(
    dataList: List<Network>,
    onItemClick: (Network) -> Unit
){
    LazyColumn {
        items(dataList) { data ->
            MyCard(data = data, onItemClick = onItemClick)
        }
    }
}

