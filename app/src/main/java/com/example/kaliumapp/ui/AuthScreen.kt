package com.example.kaliumapp.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    authType: String
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                if (authType == "register" || SharedPreferencesHelper.isNewUser(context)) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            is AuthViewModel.AuthState.Error -> {
                Toast.makeText(
                    context,
                    (authState as AuthViewModel.AuthState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Text("Kalium", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(72.dp))

        if (authType == "register") {
            Text("Create an account", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Enter your email to sign up for this app", fontSize = 14.sp)
        } else {
            Text("Welcome back", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Enter your email to sign in to your account", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (authType == "register") {
            RegisterForm(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Already have an account?",
                fontSize = 14.sp,
                color = Color(0xFF2196F3),
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Login.route)
                }
            )
        } else {
            LoginForm(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Don't have an account?",
                fontSize = 14.sp,
                color = Color(0xFF2196F3),
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        if (authState is AuthViewModel.AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = Color.LightGray, thickness = 1.dp)
            Text(" or ", modifier = Modifier.padding(8.dp), color = Color.LightGray)
            Divider(modifier = Modifier.weight(1f), color = Color.LightGray, thickness = 1.dp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        GoogleButton()
        AppleButton()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "By clicking continue, you agree to our",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Row {
            Text(
                text = "Terms of Service",
                fontSize = 12.sp,
                modifier = Modifier.clickable { /* Navigate to terms */ }
            )
            Text(
                text = " and ",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = "Privacy Policy",
                fontSize = 12.sp,
                modifier = Modifier.clickable { /* Navigate to privacy policy */ }
            )
        }
    }
}

@Composable
fun LoginForm(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login", color = Color.Black)
        }
    }
}

@Composable
fun RegisterForm(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password, confirmPassword) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register", color = Color.Black)
        }
    }
}

@Composable
fun GoogleButton() {
    Button(
        onClick = { /* Handle login with simple button */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(
            model = "https://img.icons8.com/?size=100&id=17949&format=png&color=000000",
            contentDescription = "Google",
            modifier = Modifier.size(24.dp)
        )
        Text("   Continue with Google", color = Color.Black)
    }
}

@Composable
fun AppleButton() {
    Button(
        onClick = { /* Handle login with simple button */ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(
            model = "https://img.icons8.com/?size=100&id=95294&format=png&color=000000",
            contentDescription = "Apple Logo",
            modifier = Modifier.size(24.dp),
        )
        Text("  Continue with Apple", color = Color.Black)
    }
}