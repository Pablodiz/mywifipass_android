package com.example.get_eap_tls.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.get_eap_tls.backend.peticionHTTP
import com.example.get_eap_tls.backend.certificates.procesarPeticionCertificados
import com.example.get_eap_tls.backend.wifi_connection.EapTLSConnection
import com.example.get_eap_tls.ui.theme.getEAP_TLSTheme


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
    val context = LocalContext.current

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
    content: @Composable () -> Unit

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
    ){
        content()
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


    AddEventButton(onFabClick = { showDialog = true }, content = { Text(reply) })

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
                procesarPeticionCertificados(reply)
            }
        },
        content = {
            MyTextField( 
                onTextChange = { enteredText = it }
            )
        }
    )   
}