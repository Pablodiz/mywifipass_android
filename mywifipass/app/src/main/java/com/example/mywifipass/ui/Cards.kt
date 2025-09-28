package app.mywifipass.ui.components


import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.Alignment
import app.mywifipass.model.data.Network

// i18n
import androidx.compose.ui.res.stringResource
import app.mywifipass.R

@Composable
fun InfoText(label: String, value: String) {
    if (value.isNotEmpty()) {
        Text("$label: $value")
    }
}

@Composable
fun NetworkCardInfo(network: Network) {
    Column {
        InfoText(stringResource(R.string.location), network.location)
        InfoText(stringResource(R.string.start_date), network.start_date)
        InfoText(stringResource(R.string.end_date), network.end_date)
    }
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
        
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenido principal de la card (izquierda)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.location_name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                NetworkCardInfo(network = data)
            }
            
            // Icono de estado (derecha)
            Icon(
                imageVector = if (data.is_connection_configured) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = if (data.is_connection_configured) 
                    stringResource(R.string.network_configured) 
                else 
                    stringResource(R.string.network_not_configured),
                modifier = Modifier.size(32.dp),
                tint = if (data.is_connection_configured) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
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