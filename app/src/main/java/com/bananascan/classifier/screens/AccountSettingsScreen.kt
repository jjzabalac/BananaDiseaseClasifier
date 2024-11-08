package com.bananascan.classifier.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bananascan.classifier.R
import com.bananascan.classifier.viewmodels.AccountSettingsViewModel
import com.bananascan.classifier.viewmodels.AccountSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: AccountSettingsViewModel,
    onNavigateBack: () -> Unit,  // Asegúrate de que este nombre coincida
    onDeletionCompleted: () -> Unit  // Asegúrate de que este nombre coincida
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFFF8F5D0)

    LaunchedEffect(uiState) {
        if (uiState is AccountSettingsUiState.DeletionRequested) {
            onDeletionCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is AccountSettingsUiState.Error) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(stringResource(R.string.error)) },
                    text = { Text((uiState as AccountSettingsUiState.Error).message) },
                    confirmButton = {
                        TextButton(onClick = { }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_account),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.delete_account_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showConfirmationDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.align(Alignment.End),
                        enabled = uiState !is AccountSettingsUiState.Loading
                    ) {
                        Text(stringResource(R.string.delete_my_account))
                    }
                }
            }

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text(stringResource(R.string.confirm_deletion)) },
                    text = { Text(stringResource(R.string.deletion_warning)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmationDialog = false
                                viewModel.requestAccountDeletion()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.confirm_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (uiState is AccountSettingsUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
