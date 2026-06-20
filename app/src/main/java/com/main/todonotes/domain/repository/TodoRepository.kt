package com.main.todonotes.domain.repository

import com.main.todonotes.domain.model.Todo

interface TodoRepository {
    suspend fun getTodos(): Result<List<Todo>>
    suspend fun createTodo(title: String): Result<Unit>
    suspend fun updateTodo(id: String, title: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(id: String): Result<Unit>
}
