package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Int) {
        val task = repository.getTaskById(id) ?: return
        // Stop future alarms for this id so the completed task can't trigger again.
        alarmScheduler.cancelById(id)
        // Requirement: completed tasks should be automatically deleted.
        repository.deleteTask(task)
    }
}
