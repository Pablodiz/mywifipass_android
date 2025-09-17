package app.mywifipass

import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import app.mywifipass.ui.theme.MyWifiPassTheme
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import app.mywifipass.controller.RoleController
import app.mywifipass.ui.components.TopBar
@Composable 
fun RoleScreen(){
    val context = LocalContext.current
    val roleController = remember { RoleController(context) }
    var role by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { 
                role = "Attendee"
                // Navigate to MainActivity using controller logic
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("User")
        } 
        Button(
            onClick = { 
                role = "Admin"
                // Use RoleController to determine navigation
                val intent: Intent = if (roleController.hasStoredAuthToken()) {
                    Intent(context, AdminActivity::class.java)
                } else {
                    Intent(context, LoginActivity::class.java)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Validator")
        }        
    }
}

class RoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TopBar(
                            title = "Select Role"
                        )
                        RoleScreen()
                    }
                }
            }
        }
    }
}