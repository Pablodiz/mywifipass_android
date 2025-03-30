package com.example.get_eap_tls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.Context
import android.net.wifi.WifiManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

import com.example.get_eap_tls.backend.api_petitions.WifiNetworkLocation
import com.example.get_eap_tls.backend.api_petitions.DataSource
import com.example.get_eap_tls.backend.peticionHTTP
import com.example.get_eap_tls.backend.api_petitions.processReply
import com.example.get_eap_tls.backend.wifi_connection.EapTLSConnection
import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme


@Composable
// Funcion que muestra un dialogo emergente
fun MyDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit, // Función que se ejecuta al cancelar, normalemente cerrar la ventana
    onAccept: () -> Unit, 
    dialogTitle: String = "Dialog's title",
    dialogContent: String = "This is the dialog's content",
    acceptButtonText: String = "Accept",
    cancelButtonText: String = "Cancel",
    content: @Composable () -> Unit = {Text(dialogContent)} // Utilizamos una función para poder pasarle un composable y cambiar el content
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(dialogTitle) },
            text = { content() },
            confirmButton = {
                TextButton(onClick = {
                    onAccept()
                    onDismiss()
                }) {
                    Text(acceptButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDismiss() 
                }) {
                    Text(cancelButtonText)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTextField(
    label: String = "Write your text here:",
    placeholder: String = "Your text...",
    onTextChange: (String) -> Unit

) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { 
            text = it 
            onTextChange(it)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) }
    )
    
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventButton(
    onFabClick : () -> Unit,
    content: @Composable (PaddingValues) -> Unit

) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {onFabClick()}
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add event"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ){ paddingValues -> 
        content(paddingValues)
    }
}

@Composable
fun MainScreen(){
    // Create scope for the coroutine (for async tasks)
    val scope = rememberCoroutineScope()
    // Get the context
    val context = LocalContext.current

    // Variables de la interfaz
    var reply by remember { mutableStateOf("") }
    var showDialog by  remember { mutableStateOf(false) }
    var enteredText by remember { mutableStateOf("") }
    val wifiManager = context.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager

    AddEventButton(
        onFabClick = { showDialog = true },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                //Text(reply),
                MyCardList(DataSource().loadConnections())
                    // Do something with the item click
                    // For example, show a Toast or navigate to another screen
                    //Toast.makeText(context, "Clicked on ${it.fullParsedReply.ssid}", Toast.LENGTH_SHORT).show()
                
            }
        }
    )

    MyDialog(
        showDialog = showDialog, 
        onDismiss = { 
            showDialog = false
        }, 
        onAccept = { 
            showDialog = false 
            // En un hilo secundario, hacer la petición HTTP y mostrar un mensaje de que está ocurriendo
            scope.launch{
                Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
                reply = withContext(Dispatchers.IO) {
                    try{
                        peticionHTTP(enteredText)
                    }catch (e:Exception){
                        e.message.toString()
                    }
                }
                try{
                    val wifiNetworkLocation = processReply(reply)
                    val eapTLSConnection = EapTLSConnection(wifiNetworkLocation.fullParsedReply.ssid, 
                        wifiNetworkLocation.certificates, 
                        wifiNetworkLocation.fullParsedReply.user_email, //The identity musn't have blank spaces 
                        wifiNetworkLocation.fullParsedReply.network_common_name
                    ) 
                    eapTLSConnection.connect(wifiManager)                
                }catch (e:Exception){
                    reply = e.message.toString()
                }
            }
        },
        content = {
            Column{

                MyTextField( 
                    onTextChange = { enteredText = it }
                )
            }
        }
    )   
}
@Composable
fun MyCard(
    data: WifiNetworkLocation,
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "SSID: ${data.fullParsedReply.ssid}")
            Text(text = "User Name: ${data.fullParsedReply.user_name}")
            Text(text = "User Email: ${data.fullParsedReply.user_email}")
            Text(text = "User ID Document: ${data.fullParsedReply.user_id_document}")
        }
    }
}


@Composable
fun MyCardList(
    dataList: List<WifiNetworkLocation>,
    ///onItemClick: (WifiNetworkLocation) -> Unit
){
    LazyColumn {
        items(dataList) { data ->
            MyCard(data = data)
        }
    }
}

