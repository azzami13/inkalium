package com.example.kaliumapp.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kaliumapp.model.ActivityItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuAktivitas(
    items: List<ActivityItem>,
    selectedItem: ActivityItem?,
    onItemSelected: (ActivityItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Log untuk debugging
    LaunchedEffect(items, selectedItem) {
        println("DropdownMenuAktivitas - Items: $items")
        println("DropdownMenuAktivitas - SelectedItem: $selectedItem")
    }
    LaunchedEffect(expanded) {
        println("DropdownMenuAktivitas - Expanded: $expanded")
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            println("DropdownMenuAktivitas - ExposedDropdownMenuBox toggled, expanded: $expanded")
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedItem?.name ?: "Pilih olahraga",
            onValueChange = {},
            readOnly = true,
            label = { Text("Jenis Olahraga") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                println("DropdownMenuAktivitas - Dropdown dismissed, setting expanded to false")
            }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                        println("DropdownMenuAktivitas - Item selected: ${item.name}, setting expanded to false")
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}