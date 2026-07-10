package com.main.todonotes.data.repository

import com.main.todonotes.data.local.dao.TodoDao
import com.main.todonotes.data.local.entity.TodoEntity
import com.main.todonotes.data.local.entity.toEntity
import com.main.todonotes.data.remote.api.TodoApi
import com.main.todonotes.data.remote.dto.TodoRequestDto
import com.main.todonotes.domain.model.Todo
import com.main.todonotes.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val api: TodoApi,
    private val dao: TodoDao
) : TodoRepository {

    override fun getTodos(): Flow<List<Todo>> {
        return dao.getTodos().map { entities ->
            entities.map { it.toTodo() }
        }
    }

    override suspend fun syncTodos(): Result<Unit> {
        return try {
            val dtos = api.getTodos()
            val entities = dtos.map { 
                TodoEntity(id = it.id, title = it.title, isCompleted = it.isCompleted) 
            }
            dao.refreshTodos(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTodo(title: String): Result<Unit> {
        val tempId = "temp_${UUID.randomUUID()}"
        val tempTodo = TodoEntity(id = tempId, title = title, isCompleted = false, isPendingSync = true)
        
        // Optimistic update
        dao.insertTodo(tempTodo)
        
        return try {
            val response = api.createTodo(TodoRequestDto(title))
            if (response.isSuccessful) {
                // Fetch the latest to get the real ID from the server
                syncTodos()
                Result.success(Unit)
            } else {
                // Revert on failure
                dao.deleteTodoById(tempId)
                Result.failure(Exception("Failed to create todo"))
            }
        } catch (e: Exception) {
            dao.deleteTodoById(tempId)
            Result.failure(e)
        }
    }

    override suspend fun updateTodo(id: String, title: String, isCompleted: Boolean): Result<Unit> {
        val updatedTodo = TodoEntity(id = id, title = title, isCompleted = isCompleted, isPendingSync = true)
        
        // Optimistic update
        dao.insertTodo(updatedTodo)
        
        // If it's a temp ID, we can't really update it on server yet, but assuming it's real:
        if (id.startsWith("temp_")) {
             return Result.failure(Exception("Cannot update pending todo"))
        }

        return try {
            val response = api.updateTodo(id, TodoRequestDto(title, isCompleted))
            if (response.isSuccessful) {
                // Remove pending flag by re-inserting or syncing
                dao.insertTodo(updatedTodo.copy(isPendingSync = false))
                Result.success(Unit)
            } else {
                syncTodos() // Revert local state by syncing with remote
                Result.failure(Exception("Failed to update todo"))
            }
        } catch (e: Exception) {
            syncTodos()
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(id: String): Result<Unit> {
        // Optimistic delete
        dao.deleteTodoById(id)
        
        if (id.startsWith("temp_")) return Result.success(Unit)

        return try {
            val response = api.deleteTodo(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                syncTodos() // Revert
                Result.failure(Exception("Failed to delete todo"))
            }
        } catch (e: Exception) {
            syncTodos()
            Result.failure(e)
        }
    }
}
