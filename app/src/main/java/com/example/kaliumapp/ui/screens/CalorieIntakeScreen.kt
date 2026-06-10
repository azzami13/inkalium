package com.example.kaliumapp.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.example.kaliumapp.model.FitnessActivities
import com.example.kaliumapp.model.FitnessActivity
import com.example.kaliumapp.remote.SharedPreferencesHelper
import com.example.kaliumapp.ui.components.FoodIntakeCard
import com.example.kaliumapp.ui.navigation.Screen
import com.example.kaliumapp.utils.ActivityTrackingService
import com.example.kaliumapp.viewmodel.ActivityTrackingViewModel
import com.example.kaliumapp.viewmodel.CalorieIntakeViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import kotlin.math.roundToInt

private enum class CalorieTab(val title: String) {
    Activity("Aktivitas"),
    Nutrition("Nutrisi")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieIntakeScreen(
    navController: NavController,
    token: String,
    context: Context,
    viewModel: CalorieIntakeViewModel = hiltViewModel(),
    trackingViewModel: ActivityTrackingViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val trackingState by trackingViewModel.uiState.collectAsStateWithLifecycle()
    val dailyNutrients by viewModel.dailyNutrients.collectAsStateWithLifecycle()
    val dailyIntakes by viewModel.dailyIntakes.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(CalorieTab.Activity) }

    LaunchedEffect(Unit) {
        try {
            val userId = SharedPreferencesHelper.getUserId(context)
            if (userId > 0) {
                viewModel.updateDailyNutrients(userId = userId, date = LocalDate.now(), context = context)
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Gagal memuat nutrisi: ${e.message}")
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val userId = SharedPreferencesHelper.getUserId(context)
            if (userId > 0) viewModel.refreshDailyNutrients(userId = userId, date = LocalDate.now())
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(trackingState.message) {
        trackingState.message?.let {
            snackbarHostState.showSnackbar(it)
            trackingViewModel.clearMessage()
        }
    }

    LaunchedEffect(trackingState.isTracking, trackingState.selectedActivity) {
        while (trackingState.isTracking && trackingState.selectedActivity?.needsMap == false) {
            trackingViewModel.tickStaticActivity()
            delay(1000L)
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    ActivityTrackingService.ACTION_TRACKING_UPDATE -> {
                        trackingViewModel.updateTracking(
                            elapsedSeconds = intent.getLongExtra(ActivityTrackingService.EXTRA_ELAPSED_SECONDS, 0L),
                            distanceMeters = intent.getFloatExtra(ActivityTrackingService.EXTRA_DISTANCE_METERS, 0f),
                            speedMetersPerSecond = intent.getFloatExtra(ActivityTrackingService.EXTRA_SPEED_MPS, 0f),
                            calories = intent.getDoubleExtra(ActivityTrackingService.EXTRA_CALORIES, 0.0),
                            latitude = if (intent.hasExtra(ActivityTrackingService.EXTRA_LATITUDE)) {
                                intent.getDoubleExtra(ActivityTrackingService.EXTRA_LATITUDE, 0.0)
                            } else {
                                null
                            },
                            longitude = if (intent.hasExtra(ActivityTrackingService.EXTRA_LONGITUDE)) {
                                intent.getDoubleExtra(ActivityTrackingService.EXTRA_LONGITUDE, 0.0)
                            } else {
                                null
                            }
                        )
                    }
                    ActivityTrackingService.ACTION_TRACKING_STOPPED -> {
                        val userId = SharedPreferencesHelper.getUserId(context)
                        if (userId > 0) trackingViewModel.stopTracking(userId)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(ActivityTrackingService.ACTION_TRACKING_UPDATE)
            addAction(ActivityTrackingService.ACTION_TRACKING_STOPPED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kalori") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.FoodSearch.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Cari makanan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                CalorieTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            when (selectedTab) {
                CalorieTab.Activity -> ActivityTrackingContent(
                    context = context,
                    state = trackingState,
                    onSelectActivity = trackingViewModel::selectActivity,
                    onStart = {
                        val activity = trackingState.selectedActivity
                        if (activity == null) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Pilih aktivitas dulu") }
                            return@ActivityTrackingContent
                        }
                        if (activity.needsMap && !hasLocationPermission(context)) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Izin lokasi dibutuhkan untuk tracking map") }
                            return@ActivityTrackingContent
                        }
                        trackingViewModel.startTracking()
                        if (activity.needsMap) {
                            startTrackingService(context, activity)
                        }
                    },
                    onStop = {
                        val activity = trackingState.selectedActivity
                        if (activity?.needsMap == true) {
                            context.stopService(Intent(context, ActivityTrackingService::class.java))
                        }
                        val userId = SharedPreferencesHelper.getUserId(context)
                        if (userId > 0) trackingViewModel.stopTracking(userId)
                    }
                )
                CalorieTab.Nutrition -> NutritionContent(
                    isLoading = isLoading,
                    dailyNutrients = dailyNutrients,
                    dailyIntakes = dailyIntakes,
                    viewModel = viewModel,
                    onRefresh = {
                        coroutineScope.launch {
                            val userId = SharedPreferencesHelper.getUserId(context)
                            if (userId > 0) viewModel.refreshDailyNutrients(userId = userId, date = LocalDate.now())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityTrackingContent(
    context: Context,
    state: com.example.kaliumapp.viewmodel.ActivityTrackingUiState,
    onSelectActivity: (FitnessActivity) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Pilih kegiatan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        FitnessActivities.items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { activity ->
                    ActivityChoiceCard(
                        activity = activity,
                        selected = state.selectedActivity == activity,
                        enabled = !state.isTracking,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectActivity(activity) }
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        ActivityStatsPanel(state = state)

        state.selectedActivity?.let { activity ->
            if (activity.needsMap) {
                TrackingMap(path = state.path)
            } else {
                StaticActivityPanel(activity = activity, elapsedSeconds = state.elapsedSeconds)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onStart,
                enabled = !state.isTracking,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai")
            }
            OutlinedButton(
                onClick = onStop,
                enabled = state.isTracking,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selesai")
            }
        }

        if (state.selectedActivity?.needsMap == true && !hasLocationPermission(context)) {
            Text(
                text = "Aktifkan izin lokasi agar jalur aktivitas bisa digambar di map.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ActivityChoiceCard(
    activity: FitnessActivity,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = modifier
            .height(92.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = when {
                    activity.name.contains("Lari", ignoreCase = true) -> Icons.Default.DirectionsRun
                    activity.name.contains("Sepeda", ignoreCase = true) || activity.name.contains("Bersepeda", ignoreCase = true) -> Icons.Default.DirectionsBike
                    activity.name.contains("Hiking", ignoreCase = true) -> Icons.Default.Terrain
                    else -> Icons.Default.FitnessCenter
                },
                contentDescription = null
            )
            Text(activity.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(if (activity.needsMap) "Pakai map" else "Timer", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ActivityStatsPanel(state: com.example.kaliumapp.viewmodel.ActivityTrackingUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Waktu", formatDuration(state.elapsedSeconds), Modifier.weight(1f))
                StatTile("Kalori", "${state.calories.roundToInt()} kcal", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Jarak", formatDistance(state.distanceMeters), Modifier.weight(1f))
                StatTile("Kecepatan", "%.1f km/j".format(state.speedMetersPerSecond * 3.6f), Modifier.weight(1f))
            }
            if (state.isTracking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrackingMap(path: List<LatLng>) {
    val initialPosition = path.lastOrNull() ?: LatLng(-6.200000, 106.816666)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, if (path.isEmpty()) 12f else 17f)
    }

    LaunchedEffect(path.lastOrNull()) {
        path.lastOrNull()?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 17f))
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.height(320.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (path.isNotEmpty()) {
                    Polyline(points = path, color = Color(0xFF2E7D32), width = 10f)
                    Marker(state = MarkerState(path.last()), title = "Posisi sekarang")
                }
            }
            if (path.isEmpty()) {
                Text(
                    text = "Menunggu sinyal GPS...",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StaticActivityPanel(activity: FitnessActivity, elapsedSeconds: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(activity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Aktivitas ini tidak membutuhkan map. Kalori dihitung dari MET dan durasi.")
                Text("Durasi berjalan: ${formatDuration(elapsedSeconds)}")
            }
        }
    }
}

@Composable
private fun NutritionContent(
    isLoading: Boolean,
    dailyNutrients: com.example.kaliumapp.model.NutrientInfo?,
    dailyIntakes: List<com.example.kaliumapp.data.database.entities.FoodIntake>,
    viewModel: CalorieIntakeViewModel,
    onRefresh: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daily Nutrients", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                dailyNutrients?.let { nutrients ->
                    CalorieRing(nutrients.calories, viewModel.calculateNutrientPercentage("calories", nutrients.calories))
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedNutrientBar("Carbs", viewModel.calculateNutrientPercentage("carbs", nutrients.carbs), 100f, "${nutrients.carbs.toInt()}g", Color.Blue)
                    AnimatedNutrientBar("Protein", viewModel.calculateNutrientPercentage("protein", nutrients.protein), 100f, "${nutrients.protein.toInt()}g", Color.Magenta)
                    AnimatedNutrientBar("Fat", viewModel.calculateNutrientPercentage("fat", nutrients.fat), 100f, "${nutrients.fat.toInt()}g", Color(0xFFFF9800))
                } ?: Text("Loading nutrients...")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Food Intake History", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }

        if (dailyIntakes.isEmpty()) {
            Text("No food intake recorded today", modifier = Modifier.padding(16.dp))
        } else {
            dailyIntakes.forEach {
                FoodIntakeCard(it)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CalorieRing(calories: Float, percentage: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 35f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(listOf(Color(0xFFFFEB3B), Color(0xFFFF9800))),
                startAngle = -90f,
                sweepAngle = 360f * (percentage / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Calories")
            Text("${calories.toInt()} kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${percentage.toInt()}%", fontSize = 18.sp, textAlign = TextAlign.Center)
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
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(70.dp))
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
                    .fillMaxWidth((value / maxValue).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f))))
            )
        }
        Text(displayValue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp).padding(start = 8.dp), textAlign = TextAlign.End)
    }
}

private fun startTrackingService(context: Context, activity: FitnessActivity) {
    val intent = Intent(context, ActivityTrackingService::class.java).apply {
        putExtra(ActivityTrackingService.EXTRA_ACTIVITY_NAME, activity.name)
        putExtra(ActivityTrackingService.EXTRA_MET, activity.met)
        putExtra(ActivityTrackingService.EXTRA_NEEDS_MAP, activity.needsMap)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun formatDistance(distanceMeters: Float): String {
    return if (distanceMeters >= 1000f) {
        "%.2f km".format(distanceMeters / 1000f)
    } else {
        "${distanceMeters.roundToInt()} m"
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
