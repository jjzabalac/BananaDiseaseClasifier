package com.bananascan.classifier

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
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
    private var shouldShowConfirmationDirectly = false
    private var webViewError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authRepository = AuthRepository()

        val action = intent?.action
        val data = intent?.data

        var initialUrl = "https://jjzabalac.github.io/banana-scan-deletion/"

        if (action == Intent.ACTION_VIEW && data != null) {
            when {
                data.host == "datadeletion" && data.scheme == "bananascan" -> {
                    shouldShowConfirmationDirectly = true
                    handleDeepLink(data)
                }
                data.host == "jjzabalac.github.io" &&
                        data.path?.startsWith("/banana-scan-deletion") == true -> {
                    initialUrl = data.toString()
                    shouldShowConfirmationDirectly = true
                    handleDeepLink(data)
                }
            }
        }

        setContent {
            DataDeletionScreen(
                authRepository = authRepository,
                onFinish = { finish() },
                initialUrl = initialUrl,
                showConfirmationDirectly = shouldShowConfirmationDirectly,
                webViewError = webViewError
            )
        }
    }

    private fun handleDeepLink(uri: Uri) {
        uri.getQueryParameter("action")?.let { action ->
            when (action) {
                "delete" -> shouldShowConfirmationDirectly = true
                "cancel" -> finish()
            }
        }
    }
}

@Composable
private fun DataDeletionScreen(
    authRepository: AuthRepository,
    onFinish: () -> Unit,
    initialUrl: String,
    showConfirmationDirectly: Boolean,
    webViewError: Boolean
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(showConfirmationDirectly) }
    var hasWebViewError by remember { mutableStateOf(webViewError) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val backgroundColor = Color(0xFFF8F5D0)

    LaunchedEffect(showConfirmationDirectly) {
        if (showConfirmationDirectly) {
            showConfirmDialog = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!hasWebViewError) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        userAgentString = "BananaScan/1.0"
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        cacheMode = WebSettings.LOAD_NO_CACHE
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            request?.url?.let { uri ->
                                                if (uri.scheme == "bananascan") {
                                                    showConfirmDialog = true
                                                    return true
                                                }
                                            }
                                            return false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            hasWebViewError = true
                                            isLoading = false
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {}

                                    loadUrl(initialUrl)
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
                }

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
                            onClick = { showConfirmDialog = true },
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

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text(stringResource(R.string.confirm_deletion)) },
                    text = { Text(stringResource(R.string.deletion_warning)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
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
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.confirm_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
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