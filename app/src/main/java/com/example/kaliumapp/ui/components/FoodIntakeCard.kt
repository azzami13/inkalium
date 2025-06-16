package com.example.kaliumapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kaliumapp.data.database.entities.FoodIntake

@Composable
fun FoodIntakeCard(intake: FoodIntake) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = intake.foodName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Amount: ${intake.amountGrams} g",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Calories: ${intake.calories} kcal",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Protein: ${intake.protein} g",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Fat: ${intake.fat} g",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Carbs: ${intake.carbs} g",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}