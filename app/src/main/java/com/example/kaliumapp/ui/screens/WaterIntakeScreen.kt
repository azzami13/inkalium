package com.example.kaliumapp.ui.screens

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.viewmodel.WaterIntakeViewModel
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun WaterIntakeScreen(
    navController: NavHostController,
    waterIntakeViewModel: WaterIntakeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showAddWaterDialog by remember { mutableStateOf(false) }
    var showReminderSettingsDialog by remember { mutableStateOf(false) }

    // Use collectAsStateWithLifecycle for better lifecycle handling
    val waterIntake by waterIntakeViewModel.waterIntake.collectAsStateWithLifecycle()
    val totalWaterTarget by waterIntakeViewModel.totalWaterTarget.collectAsStateWithLifecycle()
    val reminderTimes by waterIntakeViewModel.reminderTimes.collectAsStateWithLifecycle()

    // Get userId from SharedPreferences with proper error handling
    val userId = remember {
        try {
            SharedPreferencesHelper.getUserId(context).takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e("WaterIntakeScreen", "Error getting user ID", e)
            null
        }
    }

    // Load today's water data when screen loads
    LaunchedEffect(userId) {
        userId?.let { id ->
            try {
                Log.d("WaterIntakeScreen", "Loading water data for user $id")
                waterIntakeViewModel.loadTodayWaterData(id)
            } catch (e: Exception) {
                Log.e("WaterIntakeScreen", "Error loading water data", e)
            }
        }
    }

    // Refresh data when returning to screen
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            userId?.let { id ->
                try {
                    Log.d("WaterIntakeScreen", "Screen resumed - refreshing water data for user $id")
                    waterIntakeViewModel.refreshWaterData(id)
                } catch (e: Exception) {
                    Log.e("WaterIntakeScreen", "Error refreshing water data on resume", e)
                }
            }
        }
    }

    // Animated value for water intake
    val animatedValue by animateFloatAsState(
        targetValue = waterIntake.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "water_intake_animation"
    )

    val progress = if (totalWaterTarget > 0) {
        animatedValue / totalWaterTarget.toFloat().coerceAtLeast(1f)
    } else {
        0f
    }

    // Smooth wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Water Intake",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Circle with Water Animation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 120.dp,
                        color = Color(0xFF2196F3).copy(alpha = 0.2f)
                    )
                ) { showCalculatorDialog = true }
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 35f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                // Scaled-down rect for water (10% smaller)
                val scaleFactor = 0.9f
                val scaledWidth = (size.width - strokeWidth) * scaleFactor
                val scaledHeight = (size.height - strokeWidth) * scaleFactor
                val scaledOffset = (1f - scaleFactor) * (size.width - strokeWidth) / 2
                val waterRect = Rect(
                    left = strokeWidth / 2 + scaledOffset,
                    top = strokeWidth / 2 + scaledOffset,
                    right = strokeWidth / 2 + scaledOffset + scaledWidth,
                    bottom = strokeWidth / 2 + scaledOffset + scaledHeight
                )

                // Clip to the scaled-down oval
                clipPath(
                    path = Path().apply {
                        addOval(waterRect)
                    }
                ) {
                    // Water level based on progress
                    val waveAmplitude = 8f
                    val waterHeight = if (progress == 0f) {
                        waterRect.bottom + waveAmplitude // Ensure water is fully below bottom
                    } else {
                        waterRect.top + scaledHeight * (1f - progress)
                    }
                    val waveFrequency = 0.03f

                    // Draw water with wave effect
                    drawPath(
                        path = Path().apply {
                            moveTo(waterRect.left, waterRect.bottom)
                            lineTo(waterRect.left, waterHeight)
                            for (x in 0..scaledWidth.toInt()) {
                                val scaledX = x + waterRect.left
                                val y = waterHeight - sin(waveFrequency * x + wavePhase) * waveAmplitude
                                lineTo(scaledX, y)
                            }
                            lineTo(waterRect.right, waterRect.bottom)
                            close()
                        },
                        color = Color(0xFF2196F3).copy(alpha = 0.7f)
                    )
                }

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

                // Progress circle
                drawArc(
                    color = Color(0xFF2196F3),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Text(
                    text = "${animatedValue.toInt()}/${totalWaterTarget} ml",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            if (totalWaterTarget > 0) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 40.dp)
                )
            }

            if (totalWaterTarget <= 0) {
                Text(
                    text = "Tap to calculate",
                    fontSize = 14.sp,
                    color = Color.Gray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reminder Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reminders", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            IconButton(
                onClick = { showReminderSettingsDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                AsyncImage(
                    model = "https://img.icons8.com/?size=100&id=AuMLFRmG95tQ&format=png",
                    contentDescription = "Reminder Settings",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder Times
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminderTimes.size) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(6.dp)
                                    .background(
                                        color = Color(0xFF2196F3),
                                        shape = RoundedCornerShape(
                                            topStart = 8.dp,
                                            bottomStart = 8.dp,
                                        )
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 8.dp,
                                            bottomStart = 8.dp,
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AsyncImage(
                                model = "https://img.icons8.com/?size=100&id=22655&format=png",
                                contentDescription = "Reminder",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.width(100.dp))
                            Text(
                                text = reminderTimes[index],
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddWaterDialog = true },
                containerColor = Color(0xFFFFC107),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = "https://img.icons8.com/?size=100&id=xduRqLcrGaNc&format=png&color=000000",
                    contentDescription = "Add Water",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Dialogs - FIXED CALLS
    if (showCalculatorDialog) {
        WaterIntakeCalculatorDialog(
            onDismiss = { showCalculatorDialog = false },
            onCalculate = { gender, weight, age ->
                userId?.let { id ->
                    coroutineScope.launch {
                        try {
                            waterIntakeViewModel.calculateWaterIntake(id, gender, weight, age)
                            Log.d("WaterIntakeScreen", "Water target calculated for user $id")
                        } catch (e: Exception) {
                            Log.e("WaterIntakeScreen", "Error calculating water intake", e)
                        }
                    }
                }
                showCalculatorDialog = false
            }
        )
    }

    if (showAddWaterDialog) {
        AddWaterIntakeDialog(
            onDismiss = { showAddWaterDialog = false },
            onAddWater = { amount ->
                coroutineScope.launch {
                    try {
                        waterIntakeViewModel.addWaterIntake(amount)
                        Log.d("WaterIntakeScreen", "Added ${amount}ml water")
                    } catch (e: Exception) {
                        Log.e("WaterIntakeScreen", "Error adding water intake", e)
                    }
                }
            }
        )
    }

    if (showReminderSettingsDialog) {
        ReminderSettingsDialog(
            onDismiss = { showReminderSettingsDialog = false },
            onSave = { wakeUp, sleep, reminderCount ->
                coroutineScope.launch {
                    try {
                        waterIntakeViewModel.setWakeAndSleepTimes(wakeUp, sleep)
                        waterIntakeViewModel.setReminderTimes(reminderCount, reminderCount)
                        Log.d("WaterIntakeScreen", "Reminder settings saved")
                    } catch (e: Exception) {
                        Log.e("WaterIntakeScreen", "Error saving reminder settings", e)
                    }
                }
                showReminderSettingsDialog = false
            }
        )
    }
}

@Composable
fun WaterIntakeCalculatorDialog(
    onDismiss: () -> Unit,
    onCalculate: (String, Double, Int) -> Unit
) {
    var gender by remember { mutableStateOf("male") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Water Intake Calculator") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gender Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { gender = "male" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (gender == "male")
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                Color.Transparent,
                            contentColor = if (gender == "male")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (gender == "male") 2.dp else 1.dp,
                            color = if (gender == "male")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            "Male",
                            fontWeight = if (gender == "male") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    OutlinedButton(
                        onClick = { gender = "female" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (gender == "female")
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                Color.Transparent,
                            contentColor = if (gender == "female")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (gender == "female") 2.dp else 1.dp,
                            color = if (gender == "female")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            "Female",
                            fontWeight = if (gender == "female") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Weight Input
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Age Input
                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        age = it.filter { char -> char.isDigit() }
                    },
                    label = { Text("Age (years)") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weightValue = weight.toDoubleOrNull() ?: 0.0
                    val ageValue = age.toIntOrNull() ?: 0

                    if (weightValue > 0 && ageValue > 0) {
                        onCalculate(gender, weightValue, ageValue)
                    }
                }
            ) {
                Text("Calculate", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddWaterIntakeDialog(
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("200") }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Water Intake") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preset Amounts Section
                Text(
                    text = "Quick Select",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Preset Buttons
                LazyRow(
                    state = scrollState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(listOf(200, 250, 300, 350, 400, 450, 500)) { presetAmount ->
                        val isSelected = selectedPreset == presetAmount
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = presetAmount
                                amount = presetAmount.toString()
                            },
                            label = {
                                Text(
                                    "$presetAmount ml",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                disabledBorderColor = Color.Transparent,
                                disabledSelectedBorderColor = Color.Transparent,
                                borderWidth = if (isSelected) 1.dp else 0.dp,
                                selectedBorderWidth = 1.dp,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Divider with label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                // Custom Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            amount = it
                            selectedPreset = null
                        }
                    },
                    label = { Text("Custom amount") },
                    placeholder = { Text("Enter amount (50-1000 ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amount.toIntOrNull()?.let { it < 50 || it > 1000 } == true,
                    supportingText = {
                        if (amount.toIntOrNull()?.let { it < 50 || it > 1000 } == true) {
                            Text("Please enter between 50-1000 ml")
                        }
                    },
                    trailingIcon = {
                        if (amount.isNotEmpty()) {
                            IconButton(onClick = { amount = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val waterAmount = amount.toIntOrNull() ?: 200
                    onAddWater(waterAmount)
                    onDismiss()
                }
            ) {
                Text("Add", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReminderSettingsDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var wakeUpTime by remember { mutableStateOf("06:00") }
    var sleepTime by remember { mutableStateOf("22:00") }
    var reminderCount by remember { mutableStateOf(6) }

    // Time picker state
    var showWakeUpTimePicker by remember { mutableStateOf(false) }
    var showSleepTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wake Up Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wake Up Time")
                    TextButton(onClick = { showWakeUpTimePicker = true }) {
                        Text(wakeUpTime)
                    }
                }

                // Sleep Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sleep Time")
                    TextButton(onClick = { showSleepTimePicker = true }) {
                        Text(sleepTime)
                    }
                }

                // Reminder Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Number of Reminders")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (reminderCount > 1) reminderCount-- },
                            enabled = reminderCount > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(reminderCount.toString())
                        IconButton(
                            onClick = { if (reminderCount < 10) reminderCount++ },
                            enabled = reminderCount < 10
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(wakeUpTime, sleepTime, reminderCount)
                }
            ) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Time Picker for Wake Up Time
    if (showWakeUpTimePicker) {
        TimePickerDialog(
            onDismiss = { showWakeUpTimePicker = false },
            onTimeSelected = {
                wakeUpTime = it
                showWakeUpTimePicker = false
            }
        )
    }

    // Time Picker for Sleep Time
    if (showSleepTimePicker) {
        TimePickerDialog(
            onDismiss = { showSleepTimePicker = false },
            onTimeSelected = {
                sleepTime = it
                showSleepTimePicker = false
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    var hour by remember { mutableStateOf(6) }
    var minute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hour Picker
                    Column {
                        IconButton(
                            onClick = { if (hour < 23) hour++ },
                            enabled = hour < 23
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Hour")
                        }
                        Text(
                            text = String.format("%02d", hour),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(
                            onClick = { if (hour > 0) hour-- },
                            enabled = hour > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Hour")
                        }
                    }

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    // Minute Picker
                    Column {
                        IconButton(
                            onClick = { if (minute < 59) minute++ },
                            enabled = minute < 59
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Minute")
                        }
                        Text(
                            text = String.format("%02d", minute),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(
                            onClick = { if (minute > 0) minute-- },
                            enabled = minute > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Minute")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedTime = String.format("%02d:%02d", hour, minute)
                    onTimeSelected(formattedTime)
                }
            ) {
                Text("Select", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}