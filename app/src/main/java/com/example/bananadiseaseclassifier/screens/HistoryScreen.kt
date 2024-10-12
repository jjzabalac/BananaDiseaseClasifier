package com.example.bananadiseaseclassifier.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bananadiseaseclassifier.data.AuthRepository
import com.example.bananadiseaseclassifier.Classification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(authRepository: AuthRepository, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var classifications by remember { mutableStateOf<List<Classification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = authRepository.getClassificationsForUser(userId)
                result.onSuccess {
                    classifications = it
                    isLoading = false
                }.onFailure { error ->
                    errorMessage = "Failed to load history: ${error.message}"
                    isLoading = false
                }
            } else {
                errorMessage = "User not authenticated"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classification History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                classifications.isEmpty() -> {
                    Text(
                        text = "No classifications found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(classifications) { classification ->
                            ClassificationItem(classification)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationItem(classification: Classification) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Result: ${classification.result}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Confidence: ${String.format("%.2f", classification.confidence * 100)}%")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Date: ${formatDate(classification.timestamp.toDate())}")
        }
    }
}

fun formatDate(date: Date): String {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}