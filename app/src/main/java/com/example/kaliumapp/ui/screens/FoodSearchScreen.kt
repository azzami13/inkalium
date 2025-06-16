package com.example.kaliumapp.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kaliumapp.data.database.entities.FoodIntake
import com.example.kaliumapp.model.FoodItem
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.components.FoodInputDialog
import com.example.kaliumapp.ui.components.FoodItemCard
import com.example.kaliumapp.ui.components.FoodSearchBar
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.viewmodel.FoodSearchViewModel
import com.example.kaliumapp.viewmodel.CalorieIntakeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    navController: NavController,
    searchViewModel: FoodSearchViewModel = hiltViewModel(),
    calorieViewModel: CalorieIntakeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val searchResults by searchViewModel.searchResults
    val selectedFoods by searchViewModel.selectedFoods
    val isLoading by searchViewModel.isLoading.collectAsState()
    val errorMessage by searchViewModel.errorMessage.collectAsState()

    var selectedFoodForInput by remember { mutableStateOf<FoodItem?>(null) }

    // Get userId with proper error handling
    val userId = remember {
        try {
            SharedPreferencesHelper.getUserId(context).takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e("FoodSearchScreen", "Error getting user ID", e)
            null
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                searchViewModel.clearErrorMessage()
            }
        }
    }

    // Function to handle adding food intake
    fun handleAddFoodIntake(
        foodItem: FoodItem,
        amount: Float
    ) {
        userId?.let { id ->
            coroutineScope.launch {
                try {
                    // Calculate nutrients based on amount (per 100g to actual amount)
                    val calories = (foodItem.nutrients["Energy"] ?: 0.0).toFloat() * amount / 100f
                    val protein = (foodItem.nutrients["Protein"] ?: 0.0).toFloat() * amount / 100f
                    val fat = (foodItem.nutrients["Total lipid (fat)"] ?: 0.0).toFloat() * amount / 100f
                    val carbs = (foodItem.nutrients["Carbohydrate, by difference"] ?: 0.0).toFloat() * amount / 100f

                    val foodIntake = FoodIntake(
                        id = 0, // Auto-generated
                        userId = id,
                        foodName = foodItem.description,
                        calories = calories,
                        protein = protein,
                        fat = fat,
                        carbs = carbs,
                        amountGrams = amount,
                        timestamp = System.currentTimeMillis()
                    )

                    // Use the CalorieIntakeViewModel to add food intake
                    calorieViewModel.addFoodIntake(id, foodIntake)

                    Log.d("FoodSearchScreen", "Added food intake: ${foodItem.description}, ${amount}g")
                    Log.d("FoodSearchScreen", "Nutrients - Calories: $calories, Protein: $protein, Fat: $fat, Carbs: $carbs")

                    // Show success message
                    snackbarHostState.showSnackbar("Food added successfully!")

                    // Navigate back to show updated data
                    navController.popBackStack()

                } catch (e: Exception) {
                    Log.e("FoodSearchScreen", "Error adding food intake", e)
                    snackbarHostState.showSnackbar("Error adding food: ${e.message}")
                }
            }
        } ?: run {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("User not found. Please login again.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search Food") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search Bar
            FoodSearchBar(
                onSearch = { query ->
                    Log.d("FoodSearchScreen", "Searching for: $query")
                    searchViewModel.searchFood(query)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on state
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Search for food items to get started",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    Log.d("FoodSearchScreen", "Displaying ${searchResults.size} search results")
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { food ->
                            FoodItemCard(
                                foodItem = food,
                                isSelected = selectedFoods.contains(food),
                                onSelect = {
                                    selectedFoodForInput = food // Open dialog for gram input
                                }
                            )
                        }
                    }
                }
            }

            // Add selected foods button (if using multi-select)
            if (selectedFoods.isNotEmpty()) {
                Button(
                    onClick = {
                        userId?.let { id ->
                            coroutineScope.launch {
                                try {
                                    // Handle multiple selected foods
                                    // Note: This would need modification based on your exact requirements
                                    searchViewModel.logSelectedFoods(id, "your_token_here")
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    Log.e("FoodSearchScreen", "Error logging selected foods", e)
                                    snackbarHostState.showSnackbar("Error adding foods: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Selected Foods (${selectedFoods.size})")
                }
            }
        }
    }

    // Food Input Dialog
    selectedFoodForInput?.let { food ->
        FoodInputDialog(
            foodItem = food,
            onDismiss = {
                selectedFoodForInput = null
            },
            onConfirm = { grams ->
                Log.d("FoodSearchScreen", "User confirmed ${grams}g of ${food.description}")
                handleAddFoodIntake(food, grams)
                selectedFoodForInput = null
            }
        )
    }
}