package com.main.todonotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.main.todonotes.data.local.dao.TodoDao
import com.main.todonotes.data.local.entity.TodoEntity

@Database(entities = [TodoEntity::class], version = 1, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract val dao: TodoDao

    companion object {
        const val DATABASE_NAME = "todos_db"
    }
}
