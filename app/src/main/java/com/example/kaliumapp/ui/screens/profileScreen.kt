package com.example.kaliumapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.kaliumapp.viewmodel.ProfileViewModel
import com.example.kaliumapp.model.UserResponse
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun saveImageToInternalStorage(uri: Uri, context: Context): String? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)

    inputStream?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file.absolutePath
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutClick: () -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var umur by remember { mutableStateOf("") }
    var jenisKelamin by remember { mutableStateOf("") }
    var beratBadan by remember { mutableStateOf("") }
    var tinggiBadan by remember { mutableStateOf("") }
    var profileImage by remember { mutableStateOf<String?>(null) }
    var uploadedImageFileName by remember { mutableStateOf<String?>(null) }

    val jenisKelaminOptions = listOf("Male", "Female")
    var expanded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val filePath = saveImageToInternalStorage(it, context)
            filePath?.let { path ->
                profileViewModel.uploadImageToServer(path) { serverFileName ->
                    uploadedImageFileName = serverFileName
                    profileImage = serverFileName
                }
            }
        }
    }

    val profileState by profileViewModel.profileState.collectAsState()

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout? All your local data will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        profileViewModel.logout {
                            onLogoutClick()
                        }
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (profileState) {
            is ProfileViewModel.ProfileState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ProfileViewModel.ProfileState.Error -> {
                val errorMessage = (profileState as ProfileViewModel.ProfileState.Error).message
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { profileViewModel.loadUserProfile() }) {
                        Text("Try again")
                    }
                }
            }
            is ProfileViewModel.ProfileState.Success -> {
                val user = (profileState as ProfileViewModel.ProfileState.Success).user

                LaunchedEffect(user) {
                    email = user.email ?: ""
                    username = user.username ?: ""
                    umur = user.umur?.toString() ?: ""
                    jenisKelamin = user.jenisKelamin ?: ""
                    beratBadan = user.beratBadan?.toString() ?: ""
                    tinggiBadan = user.tinggiBadan?.toString() ?: ""
                    profileImage = user.profileImage
                    uploadedImageFileName = user.profileImage
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(Color.LightGray, CircleShape)
                            .clickable(enabled = isEditing) { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profileImage.isNullOrEmpty()) {
                            val imageUrl = "http://192.168.1.10:8000/static/uploads/$profileImage"
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Default photo",
                                tint = Color.Gray,
                                modifier = Modifier.size(70.dp)
                            )
                        }
                    }

                    if (isEditing) {
                        Text("Click to change photo", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { profileImage = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete photo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete photo")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(email, fontSize = 14.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = umur,
                            onValueChange = { umur = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = jenisKelamin,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                jenisKelaminOptions.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            jenisKelamin = it
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = beratBadan,
                            onValueChange = { beratBadan = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tinggiBadan,
                            onValueChange = { tinggiBadan = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val updatedUser = UserResponse(
                                    id = user.id,
                                    email = email,
                                    username = username,
                                    umur = umur.toIntOrNull() ?: 0,
                                    jenisKelamin = jenisKelamin,
                                    beratBadan = beratBadan.toDoubleOrNull() ?: 0.0,
                                    tinggiBadan = tinggiBadan.toDoubleOrNull() ?: 0.0,
                                    profileImage = uploadedImageFileName
                                )
                                profileViewModel.updateProfileToApi(updatedUser)
                                isEditing = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save change")
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Age: ${umur.ifBlank { "-" }} years old")
                            Text("Gender: ${jenisKelamin.ifBlank { "-" }}")
                            Text("Weight: ${beratBadan.ifBlank { "-" }} kg")
                            Text("Height: ${tinggiBadan.ifBlank { "-" }} cm")

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Profile")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Personal Information */ }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://img.icons8.com/?size=100&id=43942&format=png",
                                contentDescription = "Personal Information",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                            Text("   Personal Information", fontSize = 16.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Notification */ }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://img.icons8.com/?size=100&id=eMfeVHKyTnkc&format=png",
                                contentDescription = "Notification",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                            Text("   Notification", fontSize = 16.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* About */ }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://img.icons8.com/?size=100&id=lzICmAiUWSkI&format=png",
                                contentDescription = "About",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                            Text("   About", fontSize = 16.sp)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLogoutDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://img.icons8.com/?size=100&id=Q1xkcFuVON39&format=png",
                                contentDescription = "Logout",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(Color(0xFFD50000))
                            )
                            Text("   Logout", fontSize = 16.sp, color = Color.Red)
                        }
                    }
                }
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}