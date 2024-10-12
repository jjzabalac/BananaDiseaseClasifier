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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onHistoryClick: () -> Unit
) {
    val backgroundColor = Color(0xFFF8F5D0)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isClassifying by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUriState.value = uri
        uri?.let {
            try {
                bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
                resultState.value = "Image loaded. Press 'Classify' to analyze."
            } catch (e: Exception) {
                Log.e("ImagePicker", "Error loading image", e)
                resultState.value = "Error: Failed to load image"
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            try {
                imageUriState.value?.let { uri ->
                    bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                    resultState.value = "Photo captured. Press 'Classify' to analyze."
                }
            } catch (e: Exception) {
                Log.e("CameraCapture", "Error processing captured photo", e)
                resultState.value = "Error: Failed to process photo"
            }
        } else {
            resultState.value = "Failed to take photo"
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
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.app_logo5),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(48.dp)
                    )
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
                Text(
                    text = "Banana Scan",
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
                text = "Banana Disease Classifier",
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
                        text = "No image selected",
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
                Text("Upload Image", fontSize = 18.sp, color = Color.White)
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
                Text("Take Photo", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    bitmap?.let {
                        isClassifying = true
                        coroutineScope.launch {
                            classifyImage(it)
                            delay(500)
                            isClassifying = false
                        }
                    } ?: run {
                        resultState.value = "Please select or capture an image first"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                enabled = bitmap != null
            ) {
                Text("Classify", fontSize = 18.sp, color = Color.White)
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