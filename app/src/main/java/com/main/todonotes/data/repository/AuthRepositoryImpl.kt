package com.main.todonotes.data.repository

import com.main.todonotes.data.local.TokenManager
import com.main.todonotes.data.remote.api.TodoApi
import com.main.todonotes.data.remote.dto.AuthRequestDto
import com.main.todonotes.data.remote.dto.LogoutRequestDto
import com.main.todonotes.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: TodoApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val response = api.register(AuthRequestDto(email, password))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(AuthRequestDto(email, password))
            tokenManager.saveTokens(response.token, response.refreshToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenManager.refreshToken.firstOrNull()
            if (refreshToken != null) {
                api.logout(LogoutRequestDto(refreshToken))
            }
            tokenManager.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Result.success(Unit)
        }
    }

    override fun isUserLoggedIn(): Flow<Boolean> {
        return tokenManager.token.map { !it.isNullOrEmpty() }
    }
}
