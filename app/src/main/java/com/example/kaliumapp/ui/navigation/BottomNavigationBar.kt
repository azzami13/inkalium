package com.example.kaliumapp.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Water,
        Screen.CalorieIntake,
        Screen.HitungKalori
    )
    val backgroundColor = if (isSystemInDarkTheme()) {
        Color.Black
    } else {
        Color.White
    }

    NavigationBar(
        containerColor = backgroundColor
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route
        val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    val iconColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    when (screen) {
                        is Screen.Dashboard -> AsyncImage(
                            model = "https://img.icons8.com/?size=100&id=sUJRwjfnGwbJ&format=png",
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                        is Screen.Water -> AsyncImage(
                            model = "https://img.icons8.com/?size=100&id=4070&format=png",
                            contentDescription = "Water",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                        is Screen.CalorieIntake -> AsyncImage(
                            model = "https://img.icons8.com/?size=100&id=60985&format=png",
                            contentDescription = "Calorie",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                        is Screen.HitungKalori -> AsyncImage(
                            model = "https://img.icons8.com/?size=100&id=86549&format=png",
                            contentDescription = "Activity",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                        else -> {}
                    }
                },
                label = {
                    Text(
                        when (screen) {
                            is Screen.Dashboard -> "Dashboard"
                            is Screen.Water -> "Water"
                            is Screen.CalorieIntake -> "Calorie"
                            is Screen.HitungKalori -> "Activity"
                            else -> ""
                        },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}