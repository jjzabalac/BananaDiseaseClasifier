package com.example.bananadiseaseclassifier.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.bananadiseaseclassifier.R
import com.example.bananadiseaseclassifier.data.AuthRepository
import com.example.bananadiseaseclassifier.robotoCondensedItalic
import com.example.bananadiseaseclassifier.robotoCondensedSemibold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    imageUriState: MutableState<Uri?>,
    resultState: MutableState<String>,
    classifyImage: suspend (Bitmap) -> Unit,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onHistoryClick: () -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val backgroundColor = Color(0xFFF8F5D0)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isClassifying by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUriState.value = uri
        uri?.let {
            coroutineScope.launch {
                try {
                    bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    resultState.value = context.getString(R.string.image_loaded)
                } catch (e: Exception) {
                    Log.e("ImagePicker", "Error loading image", e)
                    resultState.value = context.getString(R.string.error_loading_image)
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            coroutineScope.launch {
                try {
                    imageUriState.value?.let { uri ->
                        bitmap = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                        resultState.value = context.getString(R.string.photo_captured)
                    }
                } catch (e: Exception) {
                    Log.e("CameraCapture", "Error processing captured photo", e)
                    resultState.value = context.getString(R.string.error_processing_photo)
                }
            }
        } else {
            resultState.value = context.getString(R.string.failed_to_take_photo)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.display_menu3),
                            contentDescription = stringResource(R.string.classification_history),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.app_logo5),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(48.dp)
                    )
                    Row {
                        LanguageSelector(currentLanguage, onLanguageChange)
                        IconButton(onClick = {
                            coroutineScope.launch {
                                authRepository.signOut()
                                onLogout()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.logout_logo2),
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 40.sp,
                    fontFamily = robotoCondensedSemibold,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.banana_disease_classifier),
                fontSize = 18.sp,
                fontFamily = robotoCondensedItalic,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7F),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .size(250.dp)
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                bitmap?.let { btm ->
                    Image(
                        bitmap = btm.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_image_selected),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(stringResource(R.string.upload_image), fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val photoFile = File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg")
                    val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                    imageUriState.value = photoUri
                    takePictureLauncher.launch(photoUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(stringResource(R.string.take_photo), fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    bitmap?.let {
                        isClassifying = true
                        coroutineScope.launch {
                            classifyImage(it)
                            isClassifying = false
                        }
                    } ?: run {
                        coroutineScope.launch {
                            resultState.value = context.getString(R.string.select_image_first)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                enabled = bitmap != null
            ) {
                Text(stringResource(R.string.classify), fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isClassifying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFF0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resultState.value,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(currentLanguage: String, onLanguageChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("en" to "EN", "es" to "ES")

    Box {
        IconButton(onClick = { expanded = true }) {
            Text(
                text = languages.first { it.first == currentLanguage }.second,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLanguageChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}