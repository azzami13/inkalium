package com.example.kaliumapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kaliumapp.model.FoodItem
import com.example.kaliumapp.ui.components.NutrientBar
import com.example.kaliumapp.ui.components.ProgressRing
import com.example.kaliumapp.viewmodel.FoodSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: String,
    onNavigateBack: () -> Unit,
    onAddFood: (FoodItem) -> Unit,
    viewModel: FoodSearchViewModel = hiltViewModel()
) {
    val foodItem by viewModel.selectedFood.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(foodId) {
        viewModel.getFoodDetails(foodId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Makanan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        foodItem?.let { onAddFood(it) }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Makanan")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $errorMessage")
                }
            }
            foodItem != null -> {
                val item = foodItem!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProgressRing(
                            progress = (item.nutrients["Energy"] ?: 0f) / 2000f,
                            centerText = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Calories",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${item.nutrients["Energy"]?.toInt() ?: 0} kcal",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            strokeWidth = 25.dp,
                            gradientColors = listOf(Color(0xFFFFEB3B), Color(0xFFFFC107))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NutrientBar(
                        label = "Carbs",
                        value = item.nutrients["Carbohydrate, by difference"] ?: 0f,
                        maxValue = 300f,
                        displayValue = "${item.nutrients["Carbohydrate, by difference"]?.toInt() ?: 0}g",
                        color = Color.Blue,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NutrientBar(
                        label = "Protein",
                        value = item.nutrients["Protein"] ?: 0f,
                        maxValue = 50f,
                        displayValue = "${item.nutrients["Protein"]?.toInt() ?: 0}g",
                        color = Color.Magenta,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NutrientBar(
                        label = "Fat",
                        value = item.nutrients["Total lipid (fat)"] ?: 0f,
                        maxValue = 70f,
                        displayValue = "${item.nutrients["Total lipid (fat)"]?.toInt() ?: 0}g",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No food details available")
                }
            }
        }
    }
}