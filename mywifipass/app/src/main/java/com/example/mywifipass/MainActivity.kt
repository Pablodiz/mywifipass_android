package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import android.content.Intent

import app.mywifipass.ui.theme.MyWifiPassTheme
import app.mywifipass.ui.components.MainScreenContainer
import app.mywifipass.ui.components.TopBar

// i18n
import androidx.compose.ui.res.stringResource
import app.mywifipass.R 

import app.mywifipass.controller.RoleController
import androidx.compose.ui.platform.LocalContext
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener la URL del pase WiFi si viene del deep link
        val wifiPassUrl = intent.getStringExtra("wifi_pass_url")
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var menuExpanded by remember { mutableStateOf(false) }
                    val roleController = remember { RoleController(context) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        TopBar(
                            title = stringResource(R.string.downloaded_wifi_passes), 
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
                                                Text(stringResource(R.string.loginText))
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch {
                                                    // Navigate to LoginActivity
                                                    val intent: Intent = if (roleController.hasStoredAuthToken()) {
                                                                        Intent(context, AdminActivity::class.java)
                                                                    } else {
                                                                        Intent(context, LoginActivity::class.java)
                                                                    }   
                                                    context.startActivity(intent)
                                                }
                                            }
                                        )
                                    }
                                }
                            }                            
                        )
                        MainScreenContainer(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 56.dp),
                            initialWifiPassUrl = wifiPassUrl
                        )
                    }
                }
            }
        }
    }
}