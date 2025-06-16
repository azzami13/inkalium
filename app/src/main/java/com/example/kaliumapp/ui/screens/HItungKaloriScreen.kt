package com.example.kaliumapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kaliumapp.viewmodel.HitungKaloriViewModel
import com.example.kaliumapp.ui.components.DropdownMenuAktivitas
import com.example.kaliumapp.model.HistoryItem

@Composable
fun HitungKaloriScreen(viewModel: HitungKaloriViewModel) {
    val aktivitasList = viewModel.aktivitasList
    val selected = viewModel.selectedActivity
    val durasi = viewModel.duration
    val result = viewModel.result
    val historyList = viewModel.historyList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Pilih Jenis Olahraga", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        DropdownMenuAktivitas(
            items = aktivitasList,
            selectedItem = selected,
            onItemSelected = { viewModel.selectedActivity = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = durasi,
            onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) viewModel.duration = it },
            label = { Text("Durasi (menit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.hitungKalori() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Hitung Kalori")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = result, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Histori
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Histori Aktivitas", style = MaterialTheme.typography.titleMedium)
            if (historyList.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Hapus Semua")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyList.isEmpty()) {
            Text("Belum ada histori aktivitas", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Mengambil sisa ruang
            ) {
                itemsIndexed(historyList.reversed()) { index, historyItem ->
                    HistoryItemCard(
                        historyItem = historyItem,
                        onDelete = { viewModel.removeHistoryItem(historyList.size - 1 - index) } // Sesuaikan indeks karena reversed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(historyItem: HistoryItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${historyItem.activityName} (${historyItem.duration} menit)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Kalori: %.2f kkal".format(historyItem.calories),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Waktu: ${historyItem.timestamp}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Hapus Histori"
                )
            }
        }
    }
}