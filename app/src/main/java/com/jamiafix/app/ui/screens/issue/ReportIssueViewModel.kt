package com.jamiafix.app.ui.screens.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamiafix.app.data.model.CategoryDto
import com.jamiafix.app.data.model.IssuePriority
import com.jamiafix.app.data.model.LocationDto
import com.jamiafix.app.data.repository.AuthRepository
import com.jamiafix.app.data.repository.IssueRepository
import com.jamiafix.app.data.repository.MetadataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ReportUiState(
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val locations: List<LocationDto> = emptyList()
)

class ReportIssueViewModel(
    private val issueRepository: IssueRepository,
    private val metadataRepository: MetadataRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            metadataRepository.categoriesFlow.collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
        viewModelScope.launch {
            metadataRepository.locationsFlow.collect { list ->
                _uiState.update { it.copy(locations = list) }
            }
        }
        viewModelScope.launch {
            metadataRepository.refreshMetadata()
        }
    }

    fun submitIssue(
        title: String,
        description: String,
        selectedCategory: CategoryDto?,
        selectedLocation: LocationDto?,
        priority: IssuePriority,
        photoFile: File? = null
    ) {
        if (title.isBlank() || description.isBlank() || selectedCategory == null || selectedLocation == null) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authRepository.userId.first() ?: 1
            val userName = authRepository.userName.first() ?: "Student"

            val result = issueRepository.createIssue(
                title = title.trim(),
                description = description.trim(),
                categoryId = selectedCategory.id,
                categoryName = selectedCategory.name,
                locationId = selectedLocation.id,
                locationName = selectedLocation.displayName,
                priority = priority.name,
                reporterId = userId,
                reporterName = userName
            )

            result.onSuccess { createdIssue ->
                // If photo attached and online issue id > 0, upload image
                if (photoFile != null && createdIssue.id > 0) {
                    issueRepository.uploadImage(createdIssue.id, photoFile)
                }
                _uiState.update { it.copy(isLoading = false, isSubmitted = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to report issue") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val issueRepository: IssueRepository,
        private val metadataRepository: MetadataRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportIssueViewModel(issueRepository, metadataRepository, authRepository) as T
        }
    }
}
