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
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout

import com.example.get_eap_tls.ui.components.BackButton

@Composable 
fun AdminScreen(){

}

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetEAP_TLSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopEnd), 
                            onClick = { 
                                // Handle logout action
                                val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                                sharedPreferences.edit().remove("auth_token").apply()
                                finish() 
                            }
                        ){
                            Icon(Icons.Filled.Logout, contentDescription = "Logout")   
                        }
                        BackButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopStart),
                            onClick = { finish() }
                        )
                        AdminScreen()
                    }
                }
            }
        }
    }
}
