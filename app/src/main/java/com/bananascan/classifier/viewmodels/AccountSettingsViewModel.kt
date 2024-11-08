package com.bananascan.classifier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bananascan.classifier.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AccountSettingsUiState {
    object Idle : AccountSettingsUiState()
    object Loading : AccountSettingsUiState()
    data class Error(val message: String) : AccountSettingsUiState()
    object DeletionRequested : AccountSettingsUiState()
    object DeletionCancelled : AccountSettingsUiState()
    object DeleteConfirmationNeeded : AccountSettingsUiState()
}

class AccountSettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountSettingsUiState>(AccountSettingsUiState.Idle)
    val uiState: StateFlow<AccountSettingsUiState> = _uiState

    fun requestAccountDeletion() {
        viewModelScope.launch {
            _uiState.value = AccountSettingsUiState.Loading

            authRepository.deleteUserData()
                .onSuccess {
                    _uiState.value = AccountSettingsUiState.DeletionRequested
                }
                .onFailure { error ->
                    _uiState.value = AccountSettingsUiState.Error(
                        error.message ?: "Failed to process deletion request"
                    )
                }
        }
    }

    fun cancelDeletionRequest() {
        viewModelScope.launch {
            _uiState.value = AccountSettingsUiState.Loading

            authRepository.cancelDeletionRequest()
                .onSuccess {
                    _uiState.value = AccountSettingsUiState.DeletionCancelled
                }
                .onFailure { error ->
                    _uiState.value = AccountSettingsUiState.Error(
                        error.message ?: "Failed to cancel deletion request"
                    )
                }
        }
    }

    fun showDeleteConfirmation() {
        _uiState.value = AccountSettingsUiState.DeleteConfirmationNeeded
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = AccountSettingsUiState.Idle
    }

    fun clearError() {
        if (_uiState.value is AccountSettingsUiState.Error) {
            _uiState.value = AccountSettingsUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AccountSettingsUiState.Idle
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountSettingsViewModel::class.java)) {
                return AccountSettingsViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}