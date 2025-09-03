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
import androidx.compose.ui.zIndex

import app.mywifipass.ui.theme.MyWifiPassTheme
import app.mywifipass.ui.components.MainScreenContainer
import app.mywifipass.ui.components.BackButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyWifiPassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BackButton(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(40.dp)
                                .align(Alignment.TopStart)
                                .zIndex(1f), // Ensure the button is on top
                            onClick = { finish() }
                        )
                        MainScreenContainer(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 56.dp) 
                        )
                    }
                }
            }
        }
    }
}