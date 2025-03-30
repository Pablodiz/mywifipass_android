package com.example.get_eap_tls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import android.content.Context
import android.net.wifi.WifiManager
import android.widget.Toast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

import com.example.get_eap_tls.backend.api_petitions.parseReply
import com.example.get_eap_tls.backend.database.Network
import com.example.get_eap_tls.backend.database.DataSource
import com.example.get_eap_tls.backend.peticionHTTP
import com.example.get_eap_tls.backend.wifi_connection.EapTLSConnection
import com.example.get_eap_tls.ui.theme.GetEAP_TLSTheme
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate

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
    // Get the database
    val dataSource = DataSource(context)
    
    // Variables for the UI
    var reply by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var enteredText by remember { mutableStateOf("") }
    val wifiManager = context.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    var selectedNetwork by remember { mutableStateOf<Network?>(null)}
    var showNetworkDialog by remember { mutableStateOf(false) }
    var connections by remember { mutableStateOf<List<Network>>(emptyList()) }

    // Get data from the database
    LaunchedEffect(Unit) {
        connections = withContext(Dispatchers.IO) {
            dataSource.loadConnections()
        }
    }

    AddEventButton(
        onFabClick = { showDialog = true },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                MyCardList(dataList = connections 
                ,onItemClick = { network -> 
                    selectedNetwork = network
                    showNetworkDialog = true       
                    }
                )                
            }
        }
    )
    
    // Dialog for showing the information of the selected network
    if (showNetworkDialog && selectedNetwork != null) {
        MyDialog(
            showDialog = showNetworkDialog, 
            onDismiss = { 
                showDialog = false
                selectedNetwork = null
            }, 
            onAccept = { 
                showDialog = false    
                selectedNetwork = null                             
            },
            content = {
                Column{
                    Text("User Name: ${selectedNetwork!!.user_name}")
                    Text("User Email: ${selectedNetwork!!.user_email}")
                    Text("User ID Document: ${selectedNetwork!!.user_id_document}")
                    // Button that connects to the network
                    Button(
                        onClick = {
                            scope.launch {
                                val certificates = EapTLSCertificate(
                                    selectedNetwork!!.ca_certificate.byteInputStream(),
                                    selectedNetwork!!.certificate.byteInputStream(),
                                    selectedNetwork!!.private_key.byteInputStream()
                                )
                                val eapTLSConnection = EapTLSConnection(
                                    selectedNetwork!!.ssid, 
                                    certificates, 
                                    selectedNetwork!!.user_email, //The identity musn't have blank spaces 
                                    selectedNetwork!!.network_common_name
                                ) 
                                eapTLSConnection.connect(wifiManager)                
                            }
                        }
                    ) {
                        Text("Connect")
                    }
                    // Button that the deletes this from the database
                    Button(
                        onClick = {
                            val networkToDelete = selectedNetwork
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    dataSource.deleteNetwork(networkToDelete!!)
                                    connections = dataSource.loadConnections()
                                }
                            }
                            showDialog = false
                            selectedNetwork = null
                        }
                    ) {
                        Text("Delete")
                    }
                }
            },
            dialogTitle = "${selectedNetwork!!.network_common_name}",
        )      
    }

    // Dialog for adding a new network
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
                    val network = parseReply(reply)
                    // Save the parsed reply to the database
                    withContext(Dispatchers.IO) {
                        dataSource.insertNetwork(network)//.toDatabaseModel())
                        connections = dataSource.loadConnections()
                    }             
                } catch (e:Exception){
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
    data: Network,
    onItemClick: (Network) -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick(data) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Network name: ${data.network_common_name}")
            Text("User Name: ${data.user_name}")
        }
    }
}


@Composable
fun MyCardList(
    dataList: List<Network>,
    onItemClick: (Network) -> Unit
){
    LazyColumn {
        items(dataList) { data ->
            MyCard(data = data, onItemClick = onItemClick)
        }
    }
}

