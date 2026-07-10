package com.main.todonotes.presentation.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.main.todonotes.domain.model.Todo
import com.main.todonotes.domain.repository.AuthRepository
import com.main.todonotes.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var todos by mutableStateOf<List<Todo>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _eventFlow = MutableSharedFlow<TodoEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        observeTodos()
        loadTodos()
    }

    private fun observeTodos() {
        todoRepository.getTodos().onEach { localTodos ->
            todos = localTodos
        }.launchIn(viewModelScope)
    }

    fun loadTodos() {
        viewModelScope.launch {
            isLoading = true
            val result = todoRepository.syncTodos()
            if (result.isFailure) {
                _eventFlow.emit(TodoEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to sync todos"))
            }
            isLoading = false
        }
    }

    fun createTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            // We don't set isLoading here to make it seamless
            val result = todoRepository.createTodo(title)
            if (result.isSuccess) {
                _eventFlow.emit(TodoEvent.TodoSaved)
            } else {
                _eventFlow.emit(TodoEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to create todo"))
            }
        }
    }

    fun updateTodo(id: String, title: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val result = todoRepository.updateTodo(id, title, isCompleted)
            if (result.isFailure) {
                _eventFlow.emit(TodoEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to update todo"))
            }
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            val result = todoRepository.deleteTodo(id)
            if (result.isFailure) {
                _eventFlow.emit(TodoEvent.ShowSnackbar(result.exceptionOrNull()?.message ?: "Failed to delete todo"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _eventFlow.emit(TodoEvent.LogoutSuccess)
        }
    }
}

sealed class TodoEvent {
    data class ShowSnackbar(val message: String) : TodoEvent()
    object TodoSaved : TodoEvent()
    object LogoutSuccess : TodoEvent()
}
