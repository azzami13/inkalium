package com.example.kaliumapp.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kaliumapp.model.CalorieExpenditureResponse
import com.example.kaliumapp.ui.components.CalorieHistoryGraph
import com.example.kaliumapp.viewmodel.CalorieExpenditureViewModel
import com.example.kaliumapp.utils.StepTrackingService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalorieExpenditureScreen(
    viewModel: CalorieExpenditureViewModel = hiltViewModel(),
    context: Context
) {
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val calorieHistory by viewModel.calorieHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(Unit) {
        context.startService(Intent(context, StepTrackingService::class.java))
        viewModel.registerReceiver(context)
        viewModel.fetchCalorieHistory(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            context.stopService(Intent(context, StepTrackingService::class.java))
            viewModel.unregisterReceiver(context)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Calorie Expenditure",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calories Burned Today: ${caloriesBurned.roundToInt()} kcal",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveExpenditure(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Weekly Progress",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val graphData = prepareGraphData(calorieHistory)
            CalorieHistoryGraph(
                calorieData = graphData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Last 7 Days History",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (calorieHistory.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk 7 hari terakhir",
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    items(calorieHistory) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.date,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${record.calories.roundToInt()} kcal",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun prepareGraphData(history: List<CalorieExpenditureResponse>): List<Float> {
    val today = LocalDate.now()
    val dates = (0..6).map { today.minusDays(it.toLong()) }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    return dates.map { date ->
        history.find { it.date == date.format(formatter) }?.calories ?: 0f
    }.reversed()
}