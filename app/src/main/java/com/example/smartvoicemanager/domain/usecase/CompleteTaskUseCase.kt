package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: Int) {
        repository.completeTask(id)
    }
}
