package com.example.bananadiseaseclassifier

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bananadiseaseclassifier.ui.theme.BananaDiseaseClassifierTheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontStyle
import com.example.bananadiseaseclassifier.data.AuthRepository
import com.example.bananadiseaseclassifier.screens.LoginScreen
import com.example.bananadiseaseclassifier.screens.MainScreen
import com.example.bananadiseaseclassifier.screens.RegisterScreen
import com.example.bananadiseaseclassifier.screens.HistoryScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

val robotoCondensedItalic = FontFamily(
    Font(
        resId = R.font.robotocondensed_italic,
        style = FontStyle.Italic
    )
)
val robotoCondensedSemibold = FontFamily(
    Font(
        resId = R.font.roboto_condensed_semibold,
        style = FontStyle.Normal
    )
)

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var imageUri: MutableState<Uri?>
    private var tflite: Interpreter? = null
    private lateinit var result: MutableState<String>
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        authRepository = AuthRepository()
        imageUri = mutableStateOf(null)
        result = mutableStateOf(getString(R.string.select_image_first))

        val savedLanguage = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString("Language", "en") ?: "en"
        setLocale(savedLanguage)

        setContent {
            BananaDiseaseClassifierTheme {
                var currentScreen by remember { mutableStateOf("login") }
                var currentLanguage by remember { mutableStateOf(savedLanguage) }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        authRepository = authRepository,
                        onLoginSuccess = { currentScreen = "main" },
                        onRegisterClick = { currentScreen = "register" }
                    )
                    "register" -> RegisterScreen(
                        authRepository = authRepository,
                        onRegisterSuccess = { currentScreen = "main" },
                        onLoginClick = { currentScreen = "login" }
                    )
                    "main" -> MainScreen(
                        imageUriState = imageUri,
                        resultState = result,
                        classifyImage = ::classifyImage,
                        authRepository = authRepository,
                        onLogout = { currentScreen = "login" },
                        onHistoryClick = { currentScreen = "history" },
                        currentLanguage = currentLanguage,
                        onLanguageChange = { newLanguage ->
                            setLocale(newLanguage)
                            currentLanguage = newLanguage
                            recreate()
                        }
                    )
                    "history" -> HistoryScreen(
                        authRepository = authRepository,
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }

        try {
            tflite = Interpreter(loadModelFile())
            Log.d("ModelLoading", "TFLite model loaded successfully")
            Log.d("ModelLoading", "Input shape: ${tflite?.getInputTensor(0)?.shape()?.contentToString()}")
            Log.d("ModelLoading", "Output shape: ${tflite?.getOutputTensor(0)?.shape()?.contentToString()}")
        } catch (e: Exception) {
            Log.e("ModelLoading", "Error loading TFLite model", e)
            result.value = getString(R.string.error_loading_image)
        }

        checkPermissions()
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("classification_banana_leafs-TRANSFER-LEARNING-tensorflow-lite-float32-model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private suspend fun classifyImage(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

                val outputArray = Array(1) { FloatArray(3) }
                tflite?.run(byteBuffer, outputArray)

                val endTime = System.currentTimeMillis()
                Log.d("Classification", "Time taken: ${endTime - startTime} ms")
                Log.d("Classification", "Raw results: ${outputArray[0].contentToString()}")

                val classes = arrayOf("BLACK_SIKATOGA", "HEALTHY", "MOKO")
                val probabilities = outputArray[0]

                val maxProb = probabilities.maxOrNull() ?: 0f
                val maxIndex = probabilities.indexOfFirst { it == maxProb }

                val resultString = if (maxProb < 0.6f) {
                    "DESCONOCIDO"
                } else {
                    classes[maxIndex]
                }

                val userId = authRepository.getCurrentUserId()
                Log.d("Auth", "Current user ID: $userId")

                if (userId != null) {
                    val classification = Classification(
                        userId = userId,
                        result = resultString,
                        confidence = maxProb.toDouble(),
                        timestamp = Timestamp.now()
                    )

                    try {
                        val saveResult = authRepository.saveClassification(classification)
                        saveResult.onSuccess { documentId ->
                            Log.d("Firestore", "Classification saved with ID: $documentId")
                        }.onFailure { error ->
                            Log.e("Firestore", "Error saving classification", error)
                        }
                    } catch (e: Exception) {
                        Log.e("Firestore", "Exception while saving classification", e)
                    }
                } else {
                    Log.e("Firestore", "User not authenticated, classification not saved")
                }

                result.value = getString(R.string.result, resultString) + "\n" +
                        getString(R.string.confidence, maxProb * 100)
                Log.d("Classification", "Final result: ${result.value}")
            } catch (e: Exception) {
                Log.e("Classification", "Error during classification", e)
                result.value = getString(R.string.error_loading_image)
            }
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 1 * 96 * 96 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(96 * 96)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until 96) {
            for (j in 0 until 96) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f) * 2 - 1)
                byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f) * 2 - 1)
                byteBuffer.putFloat(((value and 0xFF) / 255.0f) * 2 - 1)
            }
        }
        byteBuffer.rewind()
        return byteBuffer
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("Permissions", "All permissions granted")
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show()
            } else {
                Log.w("Permissions", "Some permissions were denied")
                Toast.makeText(this, getString(R.string.some_features_limited), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
            .putString("Language", languageCode)
            .apply()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
}

fun Context.setLocale(languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = resources.configuration
    config.setLocale(locale)
    return createConfigurationContext(config)
}

fun Activity.restartApp() {
    val intent = Intent(this, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
    finish()
}
/*/@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifierApp(
    imageUriState: MutableState<Uri?>,
    resultState: MutableState<String>,
    classifyImage: suspend (Bitmap) -> Unit
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


    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Surface(modifier = Modifier.fillMaxSize(),
                        color = backgroundColor) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Historial",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Divider()
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.display_menu3),
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.app_logo5),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .background(backgroundColor)
                        )
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .padding(end = 10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.user_logo),
                                contentDescription = "User",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "Banana Scan",
                        fontSize = 45.sp,
                        fontFamily = robotoCondensedSemibold,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 0.dp),
                        textAlign = TextAlign.Center
                    )
                }
            },
            bottomBar = {
                BottomAppBar(containerColor = backgroundColor) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                imageUriState.value = null
                                bitmap = null
                                resultState.value = "Result: Not Classified Yet"
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.try_again),
                                contentDescription = "Home",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            containerColor = backgroundColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(2.dp),
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
                    modifier = Modifier.padding(top = 0.dp, bottom = 16.dp)
                )
                                Card(
                    modifier = Modifier
                        .size(250.dp)
                        .padding(vertical = 16.dp),
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
                Spacer(modifier = Modifier.height(24.dp))
                val primaryColor = Color(0xFF4CAF50)  // Verde
                val secondaryColor = Color(0xFF2196F3)  // Azul
                val tertiaryColor = Color(0xFFFFA000)  // Amarillo

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        //.fillMaxWidth()
                        .width(300.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
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
                        //.fillMaxWidth()
                        .width(300.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = secondaryColor)
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
                        //.fillMaxWidth()
                        .width(300.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tertiaryColor),
                    enabled = bitmap != null
                ) {
                    Text("Classify", fontSize = 18.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(18.dp))
                if (isClassifying) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
                val cardBackgroundColor = Color(0xFFFFFFF0)
                Card(
                    modifier = Modifier
                        .height(90.dp)
                        .width(350.dp),
                        //.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
                    //colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resultState.value,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                                .padding(top=12.dp)
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BananaDiseaseClassifierTheme {
        ClassifierApp(
            remember { mutableStateOf(null) },
            remember { mutableStateOf("Result: Not Classified Yet") },
            {}
        )
    }
}*/