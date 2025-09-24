package app.mywifipass.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.platform.LocalContext

// i18n
import androidx.compose.ui.res.stringResource
import app.mywifipass.R

// Data classes for different types of notifications
sealed class NotificationMessage {
    data class Toast(val message: String, val duration: Int = android.widget.Toast.LENGTH_SHORT) : NotificationMessage()
    data class Dialog(val title: String, val message: String, val onDismiss: () -> Unit = {}) : NotificationMessage()
    data class Snackbar(val message: String, val actionLabel: String? = null, val onAction: () -> Unit = {}) : NotificationMessage()
    data class LoadingDialog(val message: String) : NotificationMessage()
    object HideLoadingDialog : NotificationMessage()
}

// Global notification manager
object NotificationManager {
    private val _notifications = MutableSharedFlow<NotificationMessage>()
    val notifications: SharedFlow<NotificationMessage> = _notifications

    suspend fun showToast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        _notifications.emit(NotificationMessage.Toast(message, duration))
    }

    suspend fun showDialog(title: String, message: String, onDismiss: () -> Unit = {}) {
        _notifications.emit(NotificationMessage.Dialog(title, message, onDismiss))
    }

    suspend fun showSnackbar(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        _notifications.emit(NotificationMessage.Snackbar(message, actionLabel, onAction))
    }

    suspend fun showLoadingDialog(message: String) {
        _notifications.emit(NotificationMessage.LoadingDialog(message))
    }

    suspend fun hideLoadingDialog() {
        _notifications.emit(NotificationMessage.HideLoadingDialog)
    }
}

// Non-composable functions for easy use from coroutines
object ShowText {
    suspend fun toast(message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        NotificationManager.showToast(message, duration)
    }
    
    suspend fun dialog(title: String, message: String, onDismiss: () -> Unit = {}) {
        NotificationManager.showDialog(title, message, onDismiss)
    }
    
    suspend fun snackbar(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        NotificationManager.showSnackbar(message, actionLabel, onAction)
    }
    
    suspend fun loadingDialog(message: String) {
        NotificationManager.showLoadingDialog(message)
    }
    
    suspend fun hideLoadingDialog() {
        NotificationManager.hideLoadingDialog()
    }
    
    // Fallback for contexts where you have direct access to Context
    fun toastDirect(context: Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}

// Composable that handles all notifications
@Composable
fun NotificationHandler(
    context: Context,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var currentDialog by remember { mutableStateOf<NotificationMessage.Dialog?>(null) }
    var currentLoadingDialog by remember { mutableStateOf<NotificationMessage.LoadingDialog?>(null) }
    
    // Listen to notifications
    LaunchedEffect(Unit) {
        NotificationManager.notifications.collect { notification ->
            when (notification) {
                is NotificationMessage.Toast -> {
                    Toast.makeText(context, notification.message, notification.duration).show()
                }
                is NotificationMessage.Dialog -> {
                    currentDialog = notification
                }
                is NotificationMessage.LoadingDialog -> {
                    currentLoadingDialog = notification
                }
                is NotificationMessage.HideLoadingDialog -> {
                    currentLoadingDialog = null
                }
                is NotificationMessage.Snackbar -> {
                    if (notification.actionLabel != null) {
                        val result = snackbarHostState.showSnackbar(
                            message = notification.message,
                            actionLabel = notification.actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            notification.onAction()
                        }
                    } else {
                        snackbarHostState.showSnackbar(notification.message)
                    }
                }
            }
        }
    }
    
    // Show dialog if there's one
    currentDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                val dismiss = dialog.onDismiss
                currentDialog = null
                dismiss()
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                Button(
                    onClick = {
                        val dismiss = dialog.onDismiss
                        currentDialog = null
                        dismiss()
                    }
                ) {
                    Text(stringResource(R.string.accept))
                }
            }
        )
    }
    
    // Show loading dialog if there's one
    currentLoadingDialog?.let { loadingDialog ->
        AlertDialog(
            onDismissRequest = { }, // Loading dialogs can't be dismissed by user
            title = null,
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(loadingDialog.message)
                }
            },
            confirmButton = { } // No buttons for loading dialog
        )
    }
}
