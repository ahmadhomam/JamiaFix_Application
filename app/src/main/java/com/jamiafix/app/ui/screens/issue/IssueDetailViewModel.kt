package com.jamiafix.app.ui.screens.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamiafix.app.data.model.IssueDetailDto
import com.jamiafix.app.data.model.UserDto
import com.jamiafix.app.data.model.UserRole
import com.jamiafix.app.data.repository.AuthRepository
import com.jamiafix.app.data.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val issue: IssueDetailDto? = null,
    val error: String? = null,
    val currentUserRole: UserRole = UserRole.STUDENT,
    val currentUserId: Int? = null,
    val staffList: List<UserDto> = emptyList()
)

class IssueDetailViewModel(
    private val issueId: Int,
    private val issueRepository: IssueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val role = authRepository.userRole.first()
            val userId = authRepository.userId.first()
            _uiState.update { it.copy(currentUserRole = role, currentUserId = userId) }
            loadIssueDetail()
            if (role == UserRole.ADMIN) {
                loadStaffList()
            }
        }
    }

    fun loadIssueDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = issueRepository.getIssueDetail(issueId)
            result.onSuccess { detail ->
                _uiState.update { it.copy(isLoading = false, issue = detail) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to load details") }
            }
        }
    }

    private fun loadStaffList() {
        viewModelScope.launch {
            val res = authRepository.getStaffList()
            res.onSuccess { staff ->
                _uiState.update { it.copy(staffList = staff) }
            }
        }
    }

    fun updateStatus(newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            val result = issueRepository.updateIssueStatus(issueId, newStatus, notes)
            result.onSuccess { updated ->
                _uiState.update { it.copy(isUpdating = false, issue = updated) }
            }.onFailure { err ->
                _uiState.update { it.copy(isUpdating = false, error = err.message ?: "Status transition failed") }
            }
        }
    }

    fun assignStaff(staffId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null) }
            val result = issueRepository.assignStaff(issueId, staffId)
            result.onSuccess { updated ->
                _uiState.update { it.copy(isUpdating = false, issue = updated) }
            }.onFailure { err ->
                _uiState.update { it.copy(isUpdating = false, error = err.message ?: "Staff assignment failed") }
            }
        }
    }

    fun postComment(commentText: String) {
        if (commentText.isBlank()) return
        viewModelScope.launch {
            val result = issueRepository.addComment(issueId, commentText)
            result.onSuccess {
                loadIssueDetail()
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "Failed to post comment") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val issueId: Int,
        private val issueRepository: IssueRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IssueDetailViewModel(issueId, issueRepository, authRepository) as T
        }
    }
}
