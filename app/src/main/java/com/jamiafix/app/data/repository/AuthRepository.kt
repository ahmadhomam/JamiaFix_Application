package com.jamiafix.app.data.repository

import com.jamiafix.app.data.local.UserPreferences
import com.jamiafix.app.data.model.LoginRequest
import com.jamiafix.app.data.model.RegisterRequest
import com.jamiafix.app.data.model.UserDto
import com.jamiafix.app.data.model.UserRole
import com.jamiafix.app.data.remote.JamiaFixApiService
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val apiService: JamiaFixApiService,
    private val userPreferences: UserPreferences
) {
    val isLoggedIn: Flow<Boolean> = userPreferences.isLoggedInFlow
    val userRole: Flow<UserRole> = userPreferences.userRoleFlow
    val userName: Flow<String?> = userPreferences.currentUserNameFlow
    val userEmail: Flow<String?> = userPreferences.currentUserEmailFlow
    val userId: Flow<Int?> = userPreferences.currentUserIdFlow

    suspend fun login(email: String, password: String): Result<UserDto> {
        return try {
            val response = apiService.login(LoginRequest(email.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                userPreferences.saveAuthSession(body.accessToken, body.user)
                Result.success(body.user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String, role: String): Result<UserDto> {
        return try {
            val response = apiService.register(RegisterRequest(name.trim(), email.trim(), password, role))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffList(): Result<List<UserDto>> {
        return try {
            val response = apiService.getStaffList()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch staff directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        userPreferences.clearSession()
    }
}
