package com.example.smartvoicemanager.data.repository

import com.example.smartvoicemanager.data.local.dao.TaskDao
import com.example.smartvoicemanager.data.local.entity.toDomain
import com.example.smartvoicemanager.data.local.entity.toEntity
import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> {
        return dao.getActiveTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(id: Int): Task? {
        return dao.getTaskById(id)?.toDomain()
    }

    override suspend fun insertTask(task: Task): Long {
        return dao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        dao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        dao.deleteTask(task.toEntity())
    }

    override suspend fun completeTask(id: Int) {
        dao.completeTask(id)
    }
}
