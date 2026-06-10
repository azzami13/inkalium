package com.example.kaliumapp.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.DashboardScreen
import com.example.kaliumapp.ui.auth.AuthScreen
import com.example.kaliumapp.ui.onboarding.OnboardingScreen
import com.example.kaliumapp.ui.screens.CalorieIntakeScreen
import com.example.kaliumapp.ui.screens.FoodDetailScreen
import com.example.kaliumapp.ui.screens.FoodSearchScreen
import com.example.kaliumapp.ui.screens.HitungKaloriScreen
import com.example.kaliumapp.ui.screens.ProfileScreen
import com.example.kaliumapp.ui.screens.WaterIntakeScreen
import com.example.kaliumapp.viewmodel.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Water : Screen("water")
    object Profile : Screen("profile")
    object CalorieIntake : Screen("calorie_intake")
    object HitungKalori : Screen("hitungkalori")
    object Onboarding : Screen("onboarding")
    object FoodSearch : Screen("food_search")
    object FoodDetail : Screen("food_detail/{foodId}") {
        fun createRoute(foodId: String) = "food_detail/$foodId"
    }

    @Deprecated("Gunakan Login atau Register")
    object Auth : Screen("auth")
}

@Composable
fun AppNavigation(
    context: Context,
    startDestination: String
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        if (SharedPreferencesHelper.isValidSession(context)) {
            SharedPreferencesHelper.markSessionAccessed(context)
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            if (currentDestination?.route != Screen.Login.route &&
                currentDestination?.route != Screen.Register.route &&
                currentDestination?.route != Screen.Onboarding.route &&
                currentDestination?.route != Screen.FoodSearch.route &&
                currentDestination?.route?.startsWith(Screen.FoodDetail.route) != true
            ) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                AuthScreen(
                    navController = navController,
                    viewModel = authViewModel,
                    authType = "login"
                )
            }

            composable(Screen.Register.route) {
                val authViewModel: AuthViewModel = hiltViewModel()
                AuthScreen(
                    navController = navController,
                    viewModel = authViewModel,
                    authType = "register"
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(navController)
            }

            composable(Screen.Water.route) {
                val waterIntakeViewModel: WaterIntakeViewModel = hiltViewModel()
                WaterIntakeScreen(navController = navController, waterIntakeViewModel = waterIntakeViewModel)
            }

            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                ProfileScreen(
                    onLogoutClick = {
                        profileViewModel.logout {
                            dashboardViewModel.resetData()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    },
                    profileViewModel = profileViewModel
                )
            }

            composable(Screen.HitungKalori.route) {
                val hitungKaloriViewModel: HitungKaloriViewModel = hiltViewModel()
                HitungKaloriScreen(viewModel = hitungKaloriViewModel)
            }

            composable(Screen.Onboarding.route) {
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(navController, onboardingViewModel)
            }

            composable(Screen.Auth.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }

            composable(Screen.CalorieIntake.route) {
                val token = SharedPreferencesHelper.getToken(context) ?: ""
                CalorieIntakeScreen(
                    navController = navController,
                    token = token,
                    context = context
                )
            }

            composable(Screen.FoodSearch.route) {
                FoodSearchScreen(navController = navController)
            }

            composable(
                route = Screen.FoodDetail.route,
                arguments = listOf(navArgument("foodId") { type = NavType.StringType })
            ) { backStackEntry ->
                val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
                FoodDetailScreen(
                    foodId = foodId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddFood = { foodItem ->
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
