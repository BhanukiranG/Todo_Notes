package com.main.todonotes.data.remote

import com.main.todonotes.data.local.TokenManager
import com.main.todonotes.data.remote.api.TodoApi
import com.main.todonotes.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Route
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiProvider: Provider<TodoApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = runBlocking { tokenManager.refreshToken.firstOrNull() } ?: return null

        return synchronized(this) {
            val currentToken = runBlocking { tokenManager.token.firstOrNull() }
            if (response.request.header("Authorization")?.removePrefix("Bearer ") != currentToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            try {
                val authResponse = runBlocking {
                    apiProvider.get().refresh(RefreshRequestDto(refreshToken))
                }
                
                runBlocking {
                    tokenManager.saveTokens(authResponse.token, authResponse.refreshToken)
                }

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${authResponse.token}")
                    .build()
            } catch (e: Exception) {
                runBlocking { tokenManager.clearTokens() }
                null
            }
        }
    }
}
