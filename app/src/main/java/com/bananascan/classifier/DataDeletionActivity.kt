package com.bananascan.classifier

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bananascan.classifier.data.AuthRepository
import kotlinx.coroutines.launch

class DataDeletionActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository = AuthRepository()

        setContent {
            DataDeletionScreen(
                authRepository = authRepository,
                onFinish = { finish() }
            )
        }
    }
}

@Composable
private fun DataDeletionScreen(
    authRepository: AuthRepository,
    onFinish: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val backgroundColor = Color(0xFFF8F5D0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // WebView con el contenido
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.userAgentString = "BananaScan/1.0"
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }
                                }
                                loadUrl("https://jjzabalac.github.io/banana-scan-deletion/")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Botones en la parte inferior
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    authRepository.deleteUserData()
                                        .onSuccess {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.deletion_initiated),
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onFinish()
                                        }
                                        .onFailure { e ->
                                            error = e.message
                                        }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.confirm_delete))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onFinish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }

            error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { error = null },
                    title = { Text(stringResource(R.string.error)) },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { error = null }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }
        }
    }
}