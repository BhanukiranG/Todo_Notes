package com.main.todonotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.main.todonotes.domain.model.Todo

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val isPendingSync: Boolean = false // to track offline updates
) {
    fun toTodo(): Todo {
        return Todo(
            id = id,
            title = title,
            isCompleted = isCompleted
        )
    }
}

fun Todo.toEntity(isPendingSync: Boolean = false): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        isCompleted = isCompleted,
        isPendingSync = isPendingSync
    )
}
