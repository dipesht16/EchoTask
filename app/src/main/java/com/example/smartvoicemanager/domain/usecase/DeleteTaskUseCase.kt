package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.deleteTask(task)
    }
}
