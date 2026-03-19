package com.example.smartvoicemanager.domain.repository

import com.example.smartvoicemanager.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getActiveTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Int): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun completeTask(id: Int)
}
