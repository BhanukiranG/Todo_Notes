package com.main.todonotes.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.main.todonotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    
    var isLoading by mutableStateOf(false)
        private set

    private val _eventFlow = MutableSharedFlow<AuthEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val isLoggedIn = authRepository.isUserLoggedIn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> email = event.email
            is AuthUiEvent.PasswordChanged -> password = event.password
            is AuthUiEvent.Login -> login()
            is AuthUiEvent.Register -> register()
        }
    }

    private fun login() {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            emitEvent(AuthEvent.ShowSnackbar("Email and password cannot be empty"))
            return
        }
        viewModelScope.launch {
            isLoading = true
            val result = authRepository.login(trimmedEmail, trimmedPassword)
            if (result.isSuccess) {
                _eventFlow.emit(AuthEvent.LoginSuccess)
            } else {
                emitEvent(AuthEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Login failed"))
            }
            isLoading = false
        }
    }

    private fun register() {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            emitEvent(AuthEvent.ShowSnackbar("Email and password cannot be empty"))
            return
        }
        viewModelScope.launch {
            isLoading = true
            val result = authRepository.register(trimmedEmail, trimmedPassword)
            if (result.isSuccess) {
                _eventFlow.emit(AuthEvent.RegisterSuccess)
            } else {
                emitEvent(AuthEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Registration failed"))
            }
            isLoading = false
        }
    }

    private fun emitEvent(event: AuthEvent) {
        viewModelScope.launch {
            _eventFlow.emit(event)
        }
    }
}

sealed class AuthUiEvent {
    data class EmailChanged(val email: String) : AuthUiEvent()
    data class PasswordChanged(val password: String) : AuthUiEvent()
    object Login : AuthUiEvent()
    object Register : AuthUiEvent()
}

sealed class AuthEvent {
    data class ShowSnackbar(val message: String) : AuthEvent()
    object LoginSuccess : AuthEvent()
    object RegisterSuccess : AuthEvent()
}
