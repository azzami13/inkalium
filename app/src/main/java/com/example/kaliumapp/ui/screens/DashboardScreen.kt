package com.example.kaliumapp.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.viewmodel.DashboardViewModel
import com.example.kaliumapp.ui.navigation.Screen

@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val days by dashboardViewModel.days.collectAsState()
    val waterIntake by dashboardViewModel.waterIntake.collectAsState()
    val foodCalories by dashboardViewModel.foodCalories.collectAsState()
    var exerciseTime by remember { mutableStateOf(30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== Header =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Home", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Profile
                IconButton(
                    onClick = { navController.navigate(Screen.Profile.route) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== Progress Circle =====
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                drawCircle(
                    color = Color.LightGray,
                    radius = size.minDimension / 2,
                    style = Stroke(8.dp.toPx())
                )
                drawArc(
                    color = Color(0xFFFFD700),
                    startAngle = -90f,
                    sweepAngle = (days / 10f) * 360f,
                    useCenter = false,
                    style = Stroke(8.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$days", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(text = "days", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== Icon Bar =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Activity Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(50.dp)) {
                        drawCircle(
                            color = Color.LightGray,
                            radius = size.minDimension / 2,
                            style = Stroke(4.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFFFFC107),
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(4.dp.toPx())
                        )
                    }
                    AsyncImage(
                        model = "https://img.icons8.com/?size=100&id=86549&format=png",
                        contentDescription = "Activity",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color(0xFFFFC107))
                    )
                }
                Text("Activity", fontSize = 14.sp)
            }

            // Water Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(50.dp)) {
                        drawCircle(
                            color = Color.LightGray,
                            radius = size.minDimension / 2,
                            style = Stroke(4.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF2196F3),
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(4.dp.toPx())
                        )
                    }
                    AsyncImage(
                        model = "https://img.icons8.com/?size=100&id=4070&format=png",
                        contentDescription = "Water",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF2196F3))
                    )
                }
                Text("Water", fontSize = 14.sp)
            }

            // Food Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(50.dp)) {
                        drawCircle(
                            color = Color.LightGray,
                            radius = size.minDimension / 2,
                            style = Stroke(4.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF4CAF50),
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(4.dp.toPx())
                        )
                    }
                    AsyncImage(
                        model = "https://img.icons8.com/?size=100&id=g3VPeHvpP2T3&format=png",
                        contentDescription = "Food",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF4CAF50))
                    )
                }
                Text("Food", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== Stats Card Row =====
        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
            item {
                StatCard("Exercise", "$exerciseTime mins", "+10% month over month")
                Spacer(modifier = Modifier.width(8.dp))
                StatCard("Water", "$waterIntake liters", "+33% month over month")
                Spacer(modifier = Modifier.width(8.dp))
                StatCard("Food", "$foodCalories kcal", "-8% month over month")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ===== Placeholder for Graph =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Graph", fontSize = 24.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    percentage: String
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = percentage, fontSize = 12.sp, color = Color.Gray)
        }
    }
}