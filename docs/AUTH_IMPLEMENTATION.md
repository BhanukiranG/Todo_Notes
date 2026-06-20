# Authentication & Refresh Token Implementation Guide

This document explains the robust authentication and token management system implemented in the **TodoNotes** Android application. It utilizes modern Android architecture principles with **Kotlin**, **Jetpack DataStore**, **OkHttp3**, and **Retrofit2**.

## Architecture Overview

The authentication flow relies on JSON Web Tokens (JWT) and uses a dual-token system (Access Token + Refresh Token). 
To make the developer experience seamless, token management is completely abstracted away from the UI and ViewModels. 

We rely on three core components:
1. **TokenManager**: Securely persists tokens locally.
2. **AuthInterceptor**: Injects the access token into outgoing API requests.
3. **TokenAuthenticator**: Automatically catches `401 Unauthorized` errors, refreshes the token in the background, and retries the failed request.

---

## 1. Local Storage: `TokenManager`
We use Jetpack DataStore (Preferences) to store the tokens locally. DataStore is a modern, asynchronous alternative to `SharedPreferences` based on Kotlin Coroutines and Flow.

```kotlin
class TokenManager(private val context: Context) {
    // Flow to observe the access token
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    // Suspend function to save both tokens securely
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    // ... clearTokens() and fetch tokens synchronously ...
}
```

> [!TIP]
> Reading from DataStore asynchronously via `Flow` allows the UI (`AuthViewModel`) to observe whether a user is logged in automatically, immediately redirecting them without manual state checks.

---

## 2. Attaching Tokens: `AuthInterceptor`
To ensure that protected API routes know who is making the request, we use an **OkHttp Interceptor**.

The `AuthInterceptor` intercepts every outgoing HTTP request, reads the current `accessToken` from the `TokenManager`, and injects it into the HTTP headers.

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getAccessToken() }
        val request = chain.request().newBuilder()
        
        if (!token.isNullOrEmpty()) {
            request.addHeader("Authorization", "Bearer $token")
        }
        
        return chain.proceed(request.build())
    }
}
```

> [!NOTE]
> We use `runBlocking` here because OkHttp interceptors run on a background thread. Blocking to retrieve the token from DataStore is safe and necessary to construct the header before the request proceeds.

---

## 3. Automatic Token Refresh: `TokenAuthenticator`
Access tokens expire quickly for security reasons. If a user makes a request and the server responds with **401 Unauthorized**, we need to refresh the token and try again.

We implemented OkHttp's `Authenticator` interface for this. It is specifically designed to handle authenticating failing requests automatically.

### How it works:
1. A network request fails with a `401`.
2. OkHttp calls the `TokenAuthenticator`.
3. The Authenticator reads the `refreshToken` from `TokenManager`.
4. It makes a **synchronous** API call to the `/api/auth/refresh-token` endpoint using a cloned Retrofit instance.
5. **Success:** It saves the new tokens and returns the *original* request with the new access token attached. OkHttp then automatically retries the original request.
6. **Failure:** If the refresh token itself is expired or invalid, it clears local storage, which logs the user out.

```kotlin
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val provider: Provider<TodoApi> // Lazy injection prevents circular dependency
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return null

        return synchronized(this) {
            // Make the refresh API call synchronously
            val apiResponse = runBlocking {
                provider.get().refreshToken(RefreshTokenRequest(refreshToken))
            }

            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                val newAccessToken = apiResponse.body()!!.accessToken
                val newRefreshToken = apiResponse.body()!!.refreshToken
                
                // Save new tokens
                runBlocking { tokenManager.saveTokens(newAccessToken, newRefreshToken) }
                
                // Retry the original request with the new token
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                // Refresh failed, clear session
                runBlocking { tokenManager.clearTokens() }
                null // Return null to let the 401 fail through to the UI
            }
        }
    }
}
```

> [!IMPORTANT]
> Notice the `synchronized(this)` block. If 5 network requests fail at the exact same time because the access token expired, we only want to refresh the token *once*. The synchronized block guarantees that parallel requests wait for the first refresh to finish.

---

## 4. Wiring it together (`AppModule.kt`)
Finally, we inject these interceptors into the `OkHttpClient` using Hilt Dependency Injection. 

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    authInterceptor: AuthInterceptor,
    tokenAuthenticator: TokenAuthenticator
): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(60, TimeUnit.SECONDS) // Extended timeouts
        .build()
}
```

### Summary of Benefits
* **Zero UI logic:** The Compose UI and ViewModels never have to write logic to check if a token is expired.
* **Seamless UX:** A user using the app when a token expires will experience a slight delay, but their action (e.g. creating a Todo) will still succeed without throwing an error.
* **Security:** Tokens are stored in Jetpack DataStore asynchronously.
