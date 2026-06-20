package com.main.todonotes.data.repository

import com.main.todonotes.data.remote.api.TodoApi
import com.main.todonotes.data.remote.dto.TodoRequestDto
import com.main.todonotes.domain.model.Todo
import com.main.todonotes.domain.repository.TodoRepository
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val api: TodoApi
) : TodoRepository {

    override suspend fun getTodos(): Result<List<Todo>> {
        return try {
            val dtos = api.getTodos()
            val todos = dtos.map { Todo(it.id, it.title, it.isCompleted) }
            Result.success(todos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTodo(title: String): Result<Unit> {
        return try {
            val response = api.createTodo(TodoRequestDto(title))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create todo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTodo(id: String, title: String, isCompleted: Boolean): Result<Unit> {
        return try {
            val response = api.updateTodo(id, TodoRequestDto(title, isCompleted))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update todo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(id: String): Result<Unit> {
        return try {
            val response = api.deleteTodo(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete todo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
