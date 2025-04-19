package com.example.get_eap_tls.ui.components


import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Imports for the QRScannerDialog
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.*
import com.google.zxing.*

// Imports for the NetworkDialog
import com.example.get_eap_tls.backend.wifi_connection.*
import com.example.get_eap_tls.backend.certificates.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.get_eap_tls.backend.database.*
import kotlinx.coroutines.CoroutineScope

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
fun NetworkDialog(
    showDialog: Boolean,   
    selectedNetwork: Network?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit = {},
    wifiManager: android.net.wifi.WifiManager,
    dataSource: DataSource,
    connections: List<Network>,
    scope: CoroutineScope,
    onConnectionsUpdated: (List<Network>) -> Unit = {}
){
        MyDialog(
        showDialog = showDialog, 
        onDismiss = {onDismiss()},
        onAccept = { onAccept() },
        dialogTitle = "${selectedNetwork!!.location_name}",
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
                                onConnectionsUpdated(dataSource.loadConnections())
                            }
                        }
                        onDismiss()
                    }
                ) {
                    Text("Delete")
                }
            }
        },
    )    
}