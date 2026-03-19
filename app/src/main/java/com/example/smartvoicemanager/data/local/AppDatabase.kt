package com.example.smartvoicemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartvoicemanager.data.local.dao.TaskDao
import com.example.smartvoicemanager.data.local.entity.TaskEntity

@Database(entities = [TaskEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao

    companion object {
        const val DATABASE_NAME = "smart_task_voice_reminder_db"
    }
}
