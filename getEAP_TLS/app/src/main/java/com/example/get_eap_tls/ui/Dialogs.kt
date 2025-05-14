package com.example.get_eap_tls.ui.components


import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

// Imports for the QRCode
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.*
// import androidx.compose.ui.viewinterop.AndroidView

// Imports for setting the symmetric key
import com.example.get_eap_tls.backend.api_petitions.getCertificatesSymmetricKey
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

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
fun NetworkDialogUserInfo(network: Network){
    Column {
        InfoText("User Name", network.user_name)
        InfoText("User Email", network.user_email)
        InfoText("User ID Document", network.user_id_document)
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

@Composable
fun NetworkDialogInfo(network: Network) {
    Column {
        NetworkDialogUserInfo(network)
        NetworkDialogEventInfo(network)
    }
}



@Serializable
data class QrData(
    val validation_url: String,
    val location_uuid: String 
){
    fun toJson(): String {
        return """
            {
                "location_uuid": "$location_uuid",
                "validation_url": "$validation_url"
            }
        """.trimIndent()
    }
    fun toBodyPetition(): String {
        return """
            {
                "location_uuid": "$location_uuid"
            }
        """.trimIndent()
    }
}

fun QrInfo(network: Network): String {
    val qrData = QrData(
        location_uuid = network.location_uuid,
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

fun configureConnection(network: Network): EapTLSConnection {
    val certificates = EapTLSCertificate(
        network.ca_certificate.byteInputStream(),
        network.certificate.byteInputStream(),
        network.private_key.byteInputStream()
    )
    val eapTLSConnection = EapTLSConnection(
        network.ssid,
        certificates,
        network.user_email, //The identity musn't have blank spaces
        network.network_common_name
    )
    return eapTLSConnection
}

suspend fun decryptCertificates(network: Network): String {
    return try {
        val key = hexToSecretKey(network.certificates_symmetric_key)
        val caCertificate = decryptAES256(network.ca_certificate, key)
        val certificate = decryptAES256(network.certificate, key)
        val privateKey = decryptAES256(network.private_key, key)
        if (checkCertificates(caCertificate, certificate, privateKey)) {
            network.private_key = privateKey
            network.certificate = certificate
            network.ca_certificate = caCertificate
            network.are_certificiates_decrypted = true
            "Certificates decrypted"
        } else {
            "Certificates failed"
        }
    } catch (e: Exception) {
        "Error decrypting certificates: ${e.message}"
    }
}

// Dialog that shows some information of the selected network in a QR code
// In QR Code: information from the user and http endpoints
// In text: information from the event@Composable
@Composable
fun NetworkDialog(
    showDialog: Boolean,
    selectedNetwork: Network?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    wifiManager: android.net.wifi.WifiManager,
    dataSource: DataSource,
    connections: List<Network>,
    scope: CoroutineScope,
    onConnectionsUpdated: (List<Network>) -> Unit = {},
    showToast: (String) -> Unit = {}
){
    selectedNetwork?.let {network ->
        var menuExpanded by remember { mutableStateOf(false) }
        MyDialog(
            showDialog = showDialog,
            onDismiss = { onDismiss() },
            onAccept = { 
                if (!network.is_connection_configured){
                    //var message = ""
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val symmetricKey = getCertificatesSymmetricKey(
                                    endpoint = network.certificates_symmetric_key_url, 
                                    onError = { errorMessage ->
                                        //message = errorMessage
                                    },
                                    onSuccess = { symmetricKey ->
                                        network.certificates_symmetric_key = symmetricKey
                                        network.is_certificates_key_set = true
                                        dataSource.updateNetwork(network)
                                        onConnectionsUpdated(dataSource.loadConnections())     
                                        //message = "You can now configure the connection"
                                    }
                                )
                            }
                            //showToast(message)
                        } catch (e: Exception) {
                            //showToast("Error: ${e.message}")
                        }
                        if (!network.are_certificiates_decrypted) {
                            val message = withContext(Dispatchers.IO) { 
                                decryptCertificates(network) 
                            }
                            //showToast(message)
                        }
                        if (network.are_certificiates_decrypted && !network.is_connection_configured){
                            withContext(Dispatchers.IO) {
                                try{
                                    val eapTLSConnection = configureConnection(network)
                                    eapTLSConnection.connect(wifiManager)
                                    network.is_connection_configured = true
                                    dataSource.updateNetwork(network)
                                    onConnectionsUpdated(dataSource.loadConnections())
                                    //message = "Connection ready"
                                } catch(e: Exception){
                                    //message = e.message.toString()
                                }
                            }
                            showToast("Connection ready")
                        }
                    }
                }
                onAccept() 
            },
            dialogTitle = "${network.location_name}",
            titleActions = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd)                
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = androidx.compose.ui.graphics.Color.Red
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", color = androidx.compose.ui.graphics.Color.Red)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            // Tu acción de editar aquí
                            val networkToDelete = network
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    if (networkToDelete.is_connection_configured){
                                        configureConnection(network).disconnect(wifiManager)
                                    }
                                    dataSource.deleteNetwork(networkToDelete)
                                    onConnectionsUpdated(dataSource.loadConnections())
                                }
                            }
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            },
            content = {
                Column (){
                    // QR Code with the information of the user
                    QrCode(
                        data = QrInfo(network = network),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Text with the information of the event
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false) // Use remaining space only if needed

                    ) // Its scrollable if needed
                    {
                        NetworkDialogEventInfo(network = network)
                    }
                }
            },
        )
    }
}