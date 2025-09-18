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
        
        Column(modifier = Modifier.padding(16.dp)) {
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