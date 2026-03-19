package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import javax.inject.Inject

class ScheduleTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(task: Task) {
        val taskId = repository.insertTask(task).toInt()
        val taskWithId = task.copy(id = taskId)
        alarmScheduler.schedule(taskWithId)
    }
}
