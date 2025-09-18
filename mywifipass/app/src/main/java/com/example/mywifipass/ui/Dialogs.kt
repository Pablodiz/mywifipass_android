package app.mywifipass.ui.components


import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import app.mywifipass.model.data.Network
import app.mywifipass.model.data.QrData

// Imports for the QRScannerDialog
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.*
import com.google.zxing.*

// Imports for the NetworkDialog
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.delay
// import app.mywifipass.controller.MainController
// import kotlinx.coroutines.CoroutineScope
// import androidx.compose.foundation.verticalScroll
// import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.layout.Spacer
// import androidx.compose.foundation.layout.width
// import androidx.compose.foundation.layout.height
// import androidx.compose.foundation.layout.Row
// import androidx.compose.foundation.layout.wrapContentSize
// import androidx.compose.foundation.layout.size

// Imports for the QRCode
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.*
// import androidx.compose.ui.viewinterop.AndroidView

// Imports for setting the certificates
import app.mywifipass.backend.api_petitions.getCertificates

// Import for waiting x seconds
import kotlinx.coroutines.delay

// Imports for asking for permissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast


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
    content: @Composable () -> Unit = {Text(dialogContent)}, // Utilizamos una función para poder pasarle un composable y cambiar el content
    titleActions: (@Composable () -> Unit)? = null
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dialogTitle)
                        titleActions?.let {
                            Box() {
                                it()
                            }
                        }
                    }
                }
            },
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


// Non-composable version that can be called from anywhere
fun ShowText(
    context: Context,
    text: String,
    dialog: Boolean = false,
){
    if (dialog){
        // TODO: Implement dialog version if needed
    }else{
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun QRScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermission = android.Manifest.permission.CAMERA
    var hasCameraPermission by remember { mutableStateOf(false) }

    // Launcher to request camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                // Notify the user that the permission is required
                ShowText(context, "Camera permission is required to scan QR codes")
                onDismiss()
            }
        }
    )

    // Check and request camera permission
    LaunchedEffect(Unit) {
        hasCameraPermission = context.checkSelfPermission(cameraPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(cameraPermission)
        }
    }

    if (hasCameraPermission) {
        // Show the QR scanner dialog if the permission is granted
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
                        // Start the camera decode (the user doesn't have to click another button)
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
}

@Composable
fun NetworkDialogEventInfo(network: Network) {
    Column {
        InfoText("Location", network.location)
        InfoText("Start date", network.start_date)
        InfoText("End date", network.end_date)
        InfoText("Description", network.description)
    }
}

fun QrInfo(network: Network): String {
    val qrData = QrData(
        validation_url = network.validation_url
    )
    return qrData.toJson()
}

@Composable
fun QrCode(
    data: String,
    modifier: Modifier = Modifier
) {
    // QR Code generator
    AndroidView(
        factory = { context ->
            val qrCodeWriter = QRCodeWriter()
            val hints = mapOf(
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200, hints)
            val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            ImageView(context).apply {
                setImageBitmap(bitmap)
            }
        },
        modifier = modifier
    )
}



// DEPRECATED: Not in use anymore
// Dialog that shows some information of the selected network in a QR code
// In QR Code: information from the user and http endpoints
// In text: information from the event@Composable
// @Composable
// fun NetworkDialog(
//     showDialog: Boolean,
//     selectedNetworkId: Int,
//     onDismiss: () -> Unit,
//     onAccept: () -> Unit,
//     wifiManager: android.net.wifi.WifiManager,
//     mainController: MainController,
//     scope: CoroutineScope,
//     onConnectionsUpdated: () -> Unit = {},
//     showToast: (String) -> Unit = {}
// ){
//     var currentNetwork by remember { mutableStateOf<Network?>(null) }
    
//     // Load initial network when dialog opens
//     LaunchedEffect(showDialog, selectedNetworkId) {
//         if (showDialog) {
//             val networks = mainController.getNetworks().getOrNull() ?: emptyList()
//             currentNetwork = networks.find { it.id == selectedNetworkId }
//         }
//     }

//     currentNetwork?.let {network ->
//         var menuExpanded by remember { mutableStateOf(false) }
//         var accept_text by remember(network.are_certificiates_decrypted, network.is_connection_configured) {
//             mutableStateOf(
//                 when {
//                     network.are_certificiates_decrypted && !network.is_connection_configured -> "Configure connection"
//                     network.is_connection_configured -> "Connected"
//                     else -> "Accept"
//                 }
//             )
//         } 
//         LaunchedEffect(showDialog) {
//             if (!network.is_connection_configured && !network.are_certificiates_decrypted){
//                 while (showDialog) {
//                     try {
//                         val result = mainController.downloadCertificates(network)
//                         if (result.isSuccess) {
//                             val networks = mainController.getNetworks().getOrNull() ?: emptyList()
//                             currentNetwork = networks.find { it.id == selectedNetworkId }
//                             onConnectionsUpdated()
//                             break
//                         } else {
//                             throw result.exceptionOrNull() ?: Exception("Failed to download certificates")
//                         }
//                     } catch (e: Exception) {
//                         // Continue trying
//                         // Toast.makeText(
//                         //     context,
//                         //     "${e.message}",
//                         //     Toast.LENGTH_SHORT
//                         // ).show()
//                     }
//                     delay(10_000L) // Wait 10 seconds before trying again
//                 }
//             }
//         }
//         MyDialog(
//             showDialog = showDialog,
//             onDismiss = { onDismiss() },
//             onAccept = {
//                 currentNetwork?.let { updatedNetwork ->
//                     if (!updatedNetwork.is_connection_configured && updatedNetwork.are_certificiates_decrypted) {
//                         scope.launch {
//                             val result = mainController.connectToNetwork(updatedNetwork, wifiManager)
//                             if (result.isSuccess) {
//                                 showToast("Connection configured successfully")
//                                 onConnectionsUpdated()
//                             } else {
//                                 showToast(result.exceptionOrNull()?.message ?: "Connection failed")
//                             }
//                         }
//                     } else if (!updatedNetwork.are_certificiates_decrypted) {
//                         showToast("No tan rápido")
//                     } else if (updatedNetwork.is_connection_configured) {
//                         showToast("Esque ya está configurado meu")
//                     } else{

//                     }
//                 }
//                 onAccept()
//             },
//             dialogTitle = "${network.location_name}",
//             titleActions = {
//                 IconButton(onClick = { menuExpanded = true }) {
//                     Icon(Icons.Default.MoreVert, contentDescription = "More options")
//                 }
//                 DropdownMenu(
//                     expanded = menuExpanded,
//                     onDismissRequest = { menuExpanded = false },
//                     modifier = Modifier.wrapContentSize(Alignment.TopEnd)
//                 ) {
//                     DropdownMenuItem(
//                         text = {
//                             Row(
//                                 verticalAlignment = Alignment.CenterVertically
//                             ) {
//                                 Icon(
//                                     imageVector = Icons.Default.Delete,
//                                     contentDescription = null,
//                                     modifier = Modifier.size(20.dp),
//                                     tint = androidx.compose.ui.graphics.Color.Red
//                                 )
//                                 Spacer(modifier = Modifier.width(8.dp))
//                                 Text("Delete", color = androidx.compose.ui.graphics.Color.Red)
//                             }
//                         },
//                         onClick = {
//                             menuExpanded = false
//                             scope.launch {
//                                 val result = mainController.deleteNetwork(network, wifiManager)
//                                 if (result.isSuccess) {
//                                     showToast("Network deleted successfully")
//                                     onConnectionsUpdated()
//                                 } else {
//                                     showToast(result.exceptionOrNull()?.message ?: "Delete failed")
//                                 }
//                             }
//                             onDismiss()
//                         },
//                         modifier = Modifier.padding(horizontal = 8.dp)
//                     )
//                 }
//             },
//             content = {
//                 Column (){
//                     // QR Code with the information of the user
//                     QrCode(
//                         data = QrInfo(network = network),
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .height(200.dp)
//                     )
//                     Spacer(modifier = Modifier.height(16.dp))
//                     // Text with the information of the event
//                     Box(
//                         modifier = Modifier
//                             .verticalScroll(rememberScrollState())
//                             .weight(1f, fill = false) // Use remaining space only if needed

//                     ) // Its scrollable if needed
//                     {
//                         NetworkDialogEventInfo(network = network)
//                     }
//                 }
//             },
//             acceptButtonText = accept_text
//         )
//     }
// }