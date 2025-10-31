/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

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
import app.mywifipass.ui.components.ShowText

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
// import app.mywifipass.backend.api_petitions.getCertificates

// Import for waiting x seconds
import kotlinx.coroutines.delay

// Imports for asking for permissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast

// i18n
import androidx.compose.ui.res.stringResource
import app.mywifipass.R

@Composable
// Funcion que muestra un dialogo emergente
fun MyDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit, // Función que se ejecuta al cancelar, normalemente cerrar la ventana
    onAccept: () -> Unit,
    dialogTitle: String = stringResource(R.string.default_dialog_title),
    dialogContent: String = stringResource(R.string.default_dialog_message),
    acceptButtonText: String = stringResource(R.string.ok),
    cancelButtonText: String = stringResource(R.string.cancel),
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
    onDismiss: () -> Unit,
    barcodeText: String = stringResource(R.string.scan_qr_code)
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
                ShowText.toastDirect(context, context.getString(R.string.camera_permission_required))
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
                        barcodeView.setStatusText(barcodeText)
                        // Center the status text
                        barcodeView.statusView.gravity = android.view.Gravity.CENTER
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
        InfoText(stringResource(R.string.location), network.location)
        InfoText(stringResource(R.string.start_date), network.start_date)
        InfoText(stringResource(R.string.end_date), network.end_date)
        InfoText(stringResource(R.string.description), network.description)
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
