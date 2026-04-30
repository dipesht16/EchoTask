package com.example.smartvoicemanager.domain.usecase

import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import javax.inject.Inject
import java.time.LocalDateTime

class ScheduleTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(task: Task) {
        // Prevent alarms from being scheduled for clearly past-due times.
        // The receiver will still do a final DB validation before speaking.
        val now = LocalDateTime.now()
        val graceAfterSeconds = 60L
        if (!task.isActive || task.isCompleted) {
            return
        }

        val taskId = repository.insertTask(task).toInt()
        val taskWithId = task.copy(id = taskId)
        if (task.scheduledDateTime.isBefore(now.minusSeconds(graceAfterSeconds))) {
            // Save the task for visibility, but do not create an alarm that would fire immediately.
            return
        }

        alarmScheduler.schedule(taskWithId)
    }
}
