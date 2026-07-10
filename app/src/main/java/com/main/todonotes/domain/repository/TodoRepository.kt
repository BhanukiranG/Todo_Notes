package com.main.todonotes.domain.repository

import com.main.todonotes.domain.model.Todo

import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(): Flow<List<Todo>>
    suspend fun syncTodos(): Result<Unit>
    suspend fun createTodo(title: String): Result<Unit>
    suspend fun updateTodo(id: String, title: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(id: String): Result<Unit>
}
