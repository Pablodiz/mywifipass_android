package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import app.mywifipass.ui.theme.MyWifiPassTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import app.mywifipass.ui.components.LoginField
import app.mywifipass.ui.components.PasswordField
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

import app.mywifipass.ui.components.TopBar
import app.mywifipass.ui.components.QRScannerDialog
import androidx.lifecycle.lifecycleScope

import app.mywifipass.model.data.LoginCredentials
import app.mywifipass.controller.LoginController

import app.mywifipass.ui.components.ShowText
import app.mywifipass.ui.components.NotificationHandler

// i18n
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.mywifipass.R

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var credentials by remember { mutableStateOf(LoginCredentials()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val loginController = remember { LoginController(context) }

    // Handle login function using MVC pattern
    val handleLogin: () -> Unit = {
        coroutineScope.launch {
            val result = loginController.login(credentials)
            result.fold(
                onSuccess = { message ->
                    ShowText.toastDirect(context, message)
                    context.startActivity(Intent(context, AdminActivity::class.java))
                    (context as Activity).finish()
                },
                onFailure = { exception ->
                    ShowText.dialog(title=context.getString(R.string.login_failed), message=exception.message ?: context.getString(R.string.login_failed))
                }
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 30.dp)
            .imePadding()
    ) {
        Text(stringResource(R.string.login_help_text), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        LoginField(
            value = credentials.url,
            onChange = { it -> credentials = credentials.copy(url = it) },
            leadingIcon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = "Url",
            placeholder = stringResource(R.string.url_admin_server),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        LoginField(
            value = credentials.login,
            onChange = { it -> credentials = credentials.copy(login = it) },
            leadingIcon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = stringResource(R.string.username),
            placeholder = stringResource(R.string.enter_your_login),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        PasswordField(
            value = credentials.pwd,
            onChange = { it -> credentials = credentials.copy(pwd = it) },
            submit = { handleLogin() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { handleLogin() },
            enabled = credentials.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login))
        }
    }
}

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showQrScanner by remember { mutableStateOf(false) }
                    val loginController = remember { LoginController(this@LoginActivity) }
                    
                    // Add NotificationHandler to handle loading dialogs
                    NotificationHandler(context = this@LoginActivity)
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopBar(
                            title = stringResource(R.string.login),
                            onBackClick = { finish() },
                            actions = {
                                IconButton(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(40.dp),
                                    onClick = { showQrScanner = true }
                                ) {
                                    Icon(Icons.Filled.QrCode, contentDescription = "Scan a QR")
                                }
                            }
                        )
                        LoginScreen(modifier = Modifier.weight(1f))
                    }
                    if (showQrScanner) {
                        QRScannerDialog(
                            onDismiss = { showQrScanner = false },
                            barcodeText = stringResource(R.string.login_qr_code),
                            onResult = { qrCode ->
                                lifecycleScope.launch {
                                    // Loading dialog while we don't get the message 
                                    // It has a loading circle and a processing text
                                    ShowText.loadingDialog(this@LoginActivity.getString(R.string.processing))
                                    val result = loginController.loginWithQR(qrCode)
                                    result.fold(
                                        onSuccess = { message ->
                                            ShowText.toastDirect(this@LoginActivity, message)
                                            startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                                            finish()
                                        },
                                        onFailure = { exception ->
                                            ShowText.dialog(title=this@LoginActivity.getString(R.string.login_failed), 
                                                message=exception.message ?: this@LoginActivity.getString(R.string.qr_login_failed),
                                                onDismiss = { showQrScanner = false }
                                            )
                                        }
                                    )
                                    ShowText.hideLoadingDialog()
                                }
                                showQrScanner = false
                            }
                        )
                    }
                }
            }
        }
    }
}