package com.example.get_eap_tls.ui.components


import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Imports for the QR code scanner
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.*
import com.google.zxing.*

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