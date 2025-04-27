package com.example.get_eap_tls

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import com.example.get_eap_tls.ui.components.LoginField
import com.example.get_eap_tls.ui.components.PasswordField
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.get_eap_tls.backend.api_petitions.loginPetition
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.get_eap_tls.ui.components.BackButton 

data class LoginCredentials(
    var url: String = "",
    var login: String = "",
    var pwd: String = "",
    var remember: Boolean = false
) {
    fun isNotEmpty(): Boolean {
        return login.isNotEmpty() && pwd.isNotEmpty() && url.isNotEmpty()
    }
}


suspend fun checkCredentials(
    creds: LoginCredentials, 
    context: Context,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}, 
    showToast: (String) -> Unit = {}, 
): Boolean {
    // Check credentials against the server
    return if (creds.isNotEmpty()) { 
        try {
            var message = "Checking credentials..."
            showToast(message)
            withContext(Dispatchers.IO) {           
                loginPetition(
                    creds.url,
                    creds.login,
                    creds.pwd,
                    onSuccess = { token ->
                        // Save token 
                        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putString("auth_token", token).apply()
                        onSuccess()
                        message = "Login Successful"
                    },
                    onError = { error ->
                        message = error
                        onError(error)
                    }
                )
            }
            showToast(message)
            true
        } catch (e: Exception) {
            showToast("An error occurred: ${e.message}")
            onError(e.message ?: "Unknown error")
            false
        }
    }
    else {
        showToast("Please fill in all fields")
        false
    }
}
    



@Composable
fun LoginScreen() {
    var credentials by remember { mutableStateOf(LoginCredentials()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp)
    ) {
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
            placeholder = "Enter the URL of the admin server",
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
            label = "Login",
            placeholder = "Enter your login",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        PasswordField(
            value = credentials.pwd,
            onChange = { it -> credentials = credentials.copy(pwd = it) },
            submit = {
                coroutineScope.launch {
                    checkCredentials(
                        credentials,
                        context,
                        onSuccess = {
                            context.startActivity(Intent(context, AdminActivity::class.java))
                            (context as Activity).finish()
                        },
                        onError = { credentials = LoginCredentials() },
                        showToast = {message -> 
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    checkCredentials(
                        credentials,
                        context,
                        onSuccess = {
                            context.startActivity(Intent(context, AdminActivity::class.java))
                            (context as Activity).finish()
                        },
                        onError = { credentials = LoginCredentials() }, 
                        showToast = {message -> 
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }                    )
                }
            },
            enabled = credentials.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetEAP_TLSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BackButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopStart), 
                            onClick = { finish() }
                        )
                        LoginScreen()
                    }
                }
            }
        }
    }
}