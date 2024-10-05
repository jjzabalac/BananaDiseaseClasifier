package com.example.bananadiseaseclassifier

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bananadiseaseclassifier.ui.theme.BananaDiseaseClassifierTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.io.File
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.exp

class MainActivity : ComponentActivity() {
    private lateinit var imageUri: MutableState<Uri?>
    private var tflite: Interpreter? = null
    private lateinit var result: MutableState<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUri = mutableStateOf(null)
        result = mutableStateOf("Result: Not Classified Yet")

        try {
            tflite = Interpreter(loadModelFile())
            Log.d("ModelLoading", "TFLite model loaded successfully")
            Log.d("ModelLoading", "Input shape: ${tflite?.getInputTensor(0)?.shape()?.contentToString()}")
            Log.d("ModelLoading", "Output shape: ${tflite?.getOutputTensor(0)?.shape()?.contentToString()}")
        } catch (e: Exception) {
            Log.e("ModelLoading", "Error loading TFLite model", e)
            result.value = "Error: Could not load model"
        }

        setContent {
            BananaDiseaseClassifierTheme {
                ClassifierApp(imageUri, result, ::classifyImage)
            }
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
                val maxIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
                val maxConfidence = outputArray[0][maxIndex]

                val softmaxOutputs = softmax(outputArray[0])
                Log.d("Classification", "Softmax outputs: ${softmaxOutputs.contentToString()}")

                val softmaxMaxIndex = softmaxOutputs.indices.maxByOrNull { softmaxOutputs[it] } ?: -1
                val softmaxMaxConfidence = softmaxOutputs[softmaxMaxIndex]

                result.value = if (softmaxMaxConfidence > 0.5) {
                    "Result: ${classes[softmaxMaxIndex]} (Confidence: ${String.format("%.2f", softmaxMaxConfidence)})"
                } else {
                    "Result: DESCONOCIDO (Max Confidence: ${String.format("%.2f", softmaxMaxConfidence)})"
                }

                Log.d("Classification", "Final result: ${result.value}")
            } catch (e: Exception) {
                Log.e("Classification", "Error during classification", e)
                result.value = "Error: Classification failed"
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

    private fun softmax(input: FloatArray): FloatArray {
        val output = FloatArray(input.size)
        val max = input.maxOrNull() ?: 0f
        var sum = 0f
        for (i in input.indices) {
            output[i] = exp((input[i] - max).toDouble()).toFloat()
            sum += output[i]
        }
        for (i in output.indices) {
            output[i] /= sum
        }
        return output
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
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("Permissions", "Some permissions were denied")
                Toast.makeText(this, "Some features may be limited", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
}

@Composable
fun ClassifierApp(
    imageUriState: MutableState<Uri?>,
    resultState: MutableState<String>,
    classifyImage: suspend (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUriState.value = uri
        uri?.let {
            try {
                bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
                resultState.value = "Image loaded. Press 'Play' to classify."
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
                    resultState.value = "Photo captured. Press 'Play' to classify."
                }
            } catch (e: Exception) {
                Log.e("CameraCapture", "Error processing captured photo", e)
                resultState.value = "Error: Failed to process photo"
            }
        } else {
            resultState.value = "Failed to take photo"
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val thirdHeight = screenHeight * (1f / 3f)
    val backgroundBrush = Brush.verticalGradient(
        0f to Color(0xFF4285F4),
        0.33f to Color(0xFF4285F4),
        0.33f to Color.White,
        startY = 0f,
        endY = thirdHeight.value * 10
    )

    Box(modifier = Modifier.fillMaxSize().background(brush = backgroundBrush)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Banana Scan",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Banana Disease Classifier",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7F),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 24.dp)
            )

            bitmap?.let { btm ->
                Image(
                    bitmap = btm.asImageBitmap(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
            } ?: Image(
                painter = painterResource(id = R.drawable.fondo_sin_imagen2),
                contentDescription = "Default Image",
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
            ) {
                Text(text = "Upload Image", fontSize = 20.sp, color = Color.White)
            }

            Button(
                onClick = {
                    val photoFile = File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg")
                    val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                    imageUriState.value = photoUri
                    takePictureLauncher.launch(photoUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
            ) {
                Text(text = "Take Photo", fontSize = 20.sp, color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = resultState.value,
                    fontSize = 20.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        imageUriState.value = null
                        bitmap = null
                        resultState.value = "Result: Not Classified Yet"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(3f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.home_image),
                        contentDescription = "Home",
                        tint = Color(0xFF4285F4)
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        bitmap?.let {
                            coroutineScope.launch {
                                classifyImage(it)
                            }
                        } ?: run {
                            resultState.value = "Please select or capture an image first"
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(3f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.go4_image),
                        contentDescription = "Play",
                        tint = Color(0xFF4285F4)
                    )
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
}