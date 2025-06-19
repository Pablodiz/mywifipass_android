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

@Composable 
fun RoleScreen(){
    val context = LocalContext.current
    var role by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Role", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                role = "Attendee"
                // Navigate to MainActivity
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
                // Check if there is a token in shared preferences
                val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("auth_token", null)
                val intent: Intent
                if (token != null) {
                    // Navigate to AdminActivity
                    intent = Intent(context, AdminActivity::class.java)
                } else {
                    // Navigate to LoginActivity
                    intent = Intent(context, LoginActivity::class.java)
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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RoleScreen()
                }
            }
        }
    }
}