/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.mywifipass.R
import app.mywifipass.backend.api_petitions.ApiResult

@Composable
fun ApiErrorDialog(
    apiResult: ApiResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showTechnicalDetails by remember { mutableStateOf(false) }



    if (showTechnicalDetails && apiResult.showTrace && apiResult.fullTrace != null) {
        TechnicalDetailsDialog(
            title = stringResource(R.string.technical_details_title),
            details = apiResult.fullTrace,
            onDismiss = { showTechnicalDetails = false },
            onCopy = {
                copyToClipboard(context, apiResult.fullTrace)
                ShowText.toastDirect(context, context.getString(R.string.details_copied_to_clipboard))
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(apiResult.title) },
            text = { 
                Column {
                    Text(apiResult.message)
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // if (apiResult.showTrace && apiResult.fullTrace != null) {
                    //     TextButton(
                    //         onClick = { showTechnicalDetails = true },
                    //         modifier = Modifier.padding(end = 8.dp)
                    //     ) {
                    //         Text(stringResource(R.string.show_technical_details))
                    //     }
                    // }
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        )
    }
}

@Composable
fun TechnicalDetailsDialog(
    title: String,
    details: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = details,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                )
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCopy) {
                        Text(stringResource(R.string.copy_details))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Error Details", text)
    clipboard.setPrimaryClip(clip)
}