package com.example.kaliumapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.components.FoodIntakeCard
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.viewmodel.CalorieIntakeViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieIntakeScreen(
    navController: NavController,
    token: String,
    context: Context,
    viewModel: CalorieIntakeViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dailyNutrients by viewModel.dailyNutrients.collectAsStateWithLifecycle()
    val dailyIntakes by viewModel.dailyIntakes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial load - hanya sekali saat screen pertama kali dibuka
    LaunchedEffect(Unit) {
        try {
            val userId = SharedPreferencesHelper.getUserId(context) ?: throw Exception("User ID not found")
            viewModel.updateDailyNutrients(userId = userId, date = LocalDate.now(), context = context)
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Failed to load nutrients: ${e.message}")
                viewModel.clearErrorMessage()
            }
        }
    }

    // Refresh data ketika kembali ke screen (saat onResume)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                val userId = SharedPreferencesHelper.getUserId(context) ?: return@repeatOnLifecycle
                // Gunakan refreshDailyNutrients untuk refresh cepat tanpa sync
                viewModel.refreshDailyNutrients(userId = userId, date = LocalDate.now())
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to refresh data: ${e.message}")
                }
            }
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearErrorMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Calorie Intake") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.FoodSearch.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Food")
                    }
                }
            )
        }
    ) { padding ->
        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Menambahkan scroll state untuk seluruh halaman
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Daily Nutrients",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    dailyNutrients?.let { nutrients ->
                        // Memoize calculations untuk menghindari recomposition berulang
                        val calorieProgress = remember(nutrients.calories) {
                            viewModel.calculateNutrientPercentage("calories", nutrients.calories) / 100f
                        }
                        val carbsPercent = remember(nutrients.carbs) {
                            viewModel.calculateNutrientPercentage("carbs", nutrients.carbs)
                        }
                        val proteinPercent = remember(nutrients.protein) {
                            viewModel.calculateNutrientPercentage("protein", nutrients.protein)
                        }
                        val fatPercent = remember(nutrients.fat) {
                            viewModel.calculateNutrientPercentage("fat", nutrients.fat)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animasi untuk kalori
                            val animatedCalorieProgress by animateFloatAsState(
                                targetValue = calorieProgress,
                                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                label = "calorie_progress"
                            )

                            // Animated calorie value
                            val animatedCalorieValue by animateFloatAsState(
                                targetValue = nutrients.calories,
                                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                                label = "calorie_value"
                            )

                            // Improved circle progress
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(200.dp)
                            ) {
                                Canvas(modifier = Modifier.size(200.dp)) {
                                    val strokeWidth = 35f
                                    val diameter = size.minDimension - strokeWidth
                                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                                    // Background circle
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )

                                    // Progress circle with gradient
                                    drawArc(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFFFEB3B), Color(0xFFFFC107)),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, size.height)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedCalorieProgress,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                // Center content
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Calories",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${animatedCalorieValue.toInt()} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(animatedCalorieProgress * 100).toInt()}%",
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.offset(y = 10.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Animated nutrient bars dengan optimization
                        val animatedCarbsPercent by animateFloatAsState(
                            targetValue = carbsPercent,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "carbs_percent"
                        )
                        val animatedCarbsValue by animateFloatAsState(
                            targetValue = nutrients.carbs,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "carbs_value"
                        )

                        AnimatedNutrientBar(
                            label = "Carbs",
                            value = animatedCarbsPercent,
                            maxValue = 100f,
                            displayValue = "${animatedCarbsValue.toInt()}g",
                            color = Color.Blue,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        val animatedProteinPercent by animateFloatAsState(
                            targetValue = proteinPercent,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "protein_percent"
                        )
                        val animatedProteinValue by animateFloatAsState(
                            targetValue = nutrients.protein,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "protein_value"
                        )

                        AnimatedNutrientBar(
                            label = "Protein",
                            value = animatedProteinPercent,
                            maxValue = 100f,
                            displayValue = "${animatedProteinValue.toInt()}g",
                            color = Color.Magenta,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        val animatedFatPercent by animateFloatAsState(
                            targetValue = fatPercent,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "fat_percent"
                        )
                        val animatedFatValue by animateFloatAsState(
                            targetValue = nutrients.fat,
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                            label = "fat_value"
                        )

                        AnimatedNutrientBar(
                            label = "Fat",
                            value = animatedFatPercent,
                            maxValue = 100f,
                            displayValue = "${animatedFatValue.toInt()}g",
                            color = Color(0xFFFF9800),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } ?: Text(
                        text = "Loading nutrients...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Food Intake History",
                    style = MaterialTheme.typography.titleLarge
                )

                // Refresh button
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val userId = SharedPreferencesHelper.getUserId(context) ?: return@launch
                                viewModel.refreshDailyNutrients(userId = userId, date = LocalDate.now())
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Failed to refresh: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (dailyIntakes.isEmpty()) {
                Text(
                    text = "No food intake recorded today",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Menggunakan Column dengan items manual karena LazyColumn dalam ScrollableColumn
                dailyIntakes.forEach { intake ->
                    FoodIntakeCard(intake)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedNutrientBar(
    label: String,
    value: Float,
    maxValue: Float,
    displayValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value / maxValue)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.7f))
                        )
                    )
            )
        }
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(50.dp).padding(start = 8.dp),
            textAlign = TextAlign.End
        )
    }
}