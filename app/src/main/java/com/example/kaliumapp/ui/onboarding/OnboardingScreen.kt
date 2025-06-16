package com.example.kaliumapp.ui.onboarding

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Log.d("OnboardingScreen", "OnboardingScreen composed")
    }

    var step by remember { mutableStateOf(0) }
    val username by onboardingViewModel.username.collectAsState()
    val umur by onboardingViewModel.umur.collectAsState()
    val jenisKelamin by onboardingViewModel.jenisKelamin.collectAsState()
    val beratBadan by onboardingViewModel.beratBadan.collectAsState()
    val tinggiBadan by onboardingViewModel.tinggiBadan.collectAsState()
    val saveState by onboardingViewModel.saveState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Handle save state
    LaunchedEffect(saveState) {
        when (saveState) {
            is OnboardingViewModel.SaveState.Success -> {
                SharedPreferencesHelper.clearNewUser(context)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
            is OnboardingViewModel.SaveState.Error -> {
                // Error sudah di-handle di UI
            }
            else -> {}
        }
    }

    val onboardingSteps = listOf(
        OnboardingStep("Hey there! What should we call you?", "Your name will show up on your profile and in your activity history.", Icons.Default.Person),
        OnboardingStep("Mind sharing your age?", "It helps us figure out how many calories you need each day.", Icons.Default.Cake),
        OnboardingStep("Let us know your gender!", "It helps us get a more accurate read on your BMR.", Icons.Default.Face),
        OnboardingStep("How much do you weigh?", "It helps us figure out your BMI and what your body needs nutritionally.", Icons.Default.LineWeight),
        OnboardingStep("Mind telling us your height?", "It helps us work out your BMI and ideal body proportions.", Icons.Default.Height),
        OnboardingStep("You're nearly done!", "Get ready to start your path toward a healthier, happier you!", Icons.Default.CheckCircle)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OnboardingProgressIndicator(
            totalSteps = onboardingSteps.size,
            currentStep = step,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Show error message if save failed
        if (saveState is OnboardingViewModel.SaveState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = (saveState as OnboardingViewModel.SaveState.Error).message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() with
                            slideOutHorizontally { width -> width } + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { targetStep ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Icon(
                    imageVector = onboardingSteps[targetStep].icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(64.dp))

                Text(
                    text = onboardingSteps[targetStep].title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = onboardingSteps[targetStep].description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                when (targetStep) {
                    0 -> UsernameInputField(username, onboardingViewModel)
                    1 -> AgeInputField(umur, onboardingViewModel)
                    2 -> GenderSelectionField(jenisKelamin, onboardingViewModel)
                    3 -> WeightInputField(beratBadan, onboardingViewModel)
                    4 -> HeightInputField(tinggiBadan, onboardingViewModel)
                    5 -> TermsAndConditionsSection()
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        NavigationButtons(
            currentStep = step,
            totalSteps = onboardingSteps.size,
            isLoading = saveState is OnboardingViewModel.SaveState.Saving,
            onNext = {
                if (step < onboardingSteps.size - 1) {
                    step++
                } else {
                    coroutineScope.launch {
                        onboardingViewModel.saveOnboardingData()
                    }
                }
            },
            onPrevious = { if (step > 0) step-- },
            isLastStep = step == onboardingSteps.size - 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// Komponen lainnya tetap sama, hanya tambahkan isLoading di NavigationButtons
@Composable
fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    isLoading: Boolean = false,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onNext,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isLastStep) "Start Journey" else "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isLastStep) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (currentStep > 0 && !isLoading) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Go Back")
            }
        }
    }
}

// Komponen input field tetap sama seperti sebelumnya...
@Composable
fun OnboardingProgressIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 0 until totalSteps) {
            val isActive = i <= currentStep
            val indicatorColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val indicatorSize = if (i == currentStep) 12.dp else 8.dp

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(indicatorSize)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameInputField(
    username: String,
    onboardingViewModel: OnboardingViewModel
) {
    OutlinedTextField(
        value = username,
        onValueChange = { onboardingViewModel.updateUsername(it) },
        label = { Text("Your Name") },
        placeholder = { Text("Enter your name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeInputField(
    age: String,
    onboardingViewModel: OnboardingViewModel
) {
    OutlinedTextField(
        value = age,
        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) onboardingViewModel.updateUmur(it) },
        label = { Text("Your Age") },
        placeholder = { Text("Type in your current age", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun GenderSelectionField(
    selectedGender: String,
    onboardingViewModel: OnboardingViewModel
) {
    val options = listOf("Male", "Female")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedGender == option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = { onboardingViewModel.updateJenisKelamin(option) }
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )

                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = if (option == "Male") Icons.Default.Male else Icons.Default.Female,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputField(
    weight: String,
    onboardingViewModel: OnboardingViewModel
) {
    OutlinedTextField(
        value = weight,
        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onboardingViewModel.updateBeratBadan(it) },
        label = { Text("Your Weight") },
        placeholder = { Text("Type in your current weight", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.LineWeight, contentDescription = null) },
        trailingIcon = { Text("kg", modifier = Modifier.padding(end = 8.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeightInputField(
    height: String,
    onboardingViewModel: OnboardingViewModel
) {
    OutlinedTextField(
        value = height,
        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onboardingViewModel.updateTinggiBadan(it) },
        label = { Text("Your Height") },
        placeholder = { Text("Type in your height here", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) },
        trailingIcon = { Text("cm", modifier = Modifier.padding(end = 8.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun TermsAndConditionsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "By continuing, you agree to:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                TermsItem("Terms & Conditions")
                TermsItem("Privacy Policy")
                TermsItem("Health Data Access Permission")
            }
        }
    }
}

@Composable
fun TermsItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)