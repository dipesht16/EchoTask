package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getActiveTasks()
    }
}
