package com.example.get_eap_tls

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.setContent

import com.example.get_eap_tls.ui.theme.getEAP_TLSTheme
import com.example.get_eap_tls.ui.components.MainScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(34)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            getEAP_TLSTheme {
                // A surface container using the 'background' color from the theme  
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {   
                    MainScreen() 
                }
            }
        }
    }
}

