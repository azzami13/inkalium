package com.example.kaliumapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.kaliumapp.model.FoodItem

@Composable
fun FoodInputDialog(
    foodItem: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var gramsInput by remember { mutableStateOf("") }
    val isValidInput = gramsInput.toFloatOrNull()?.let { it > 0 } ?: false

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Input Amount for ${foodItem.description}",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = gramsInput,
                    onValueChange = { gramsInput = it },
                    label = { Text("Amount (grams)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = gramsInput.isNotEmpty() && !isValidInput
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { onConfirm(gramsInput.toFloat()) },
                        enabled = isValidInput
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}