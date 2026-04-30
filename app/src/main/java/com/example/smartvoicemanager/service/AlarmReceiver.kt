package com.example.smartvoicemanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId == -1) return

        // Keep extras as fallback only; the receiver primarily uses DB values for reliability.
        val fallbackTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val fallbackDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskRepository.getTaskById(taskId)

                if (task == null) {
                    // Stale alarm (task was deleted elsewhere).
                    alarmScheduler.cancelById(taskId)
                    return@launch
                }

                // Never notify for completed/inactive tasks.
                if (!task.isActive || task.isCompleted) {
                    alarmScheduler.cancelById(taskId)
                    // Ensure completed tasks are fully removed.
                    if (task.isCompleted || !task.isActive) {
                        taskRepository.deleteTask(task)
                    }
                    return@launch
                }

                val now = LocalDateTime.now()
                val diff = Duration.between(task.scheduledDateTime, now)

                val graceBefore = Duration.ofSeconds(GRACE_BEFORE_SECONDS)
                val graceAfter = Duration.ofSeconds(GRACE_AFTER_SECONDS)
                val isValidTime =
                    diff >= graceBefore.negated() && diff <= graceAfter

                // Gate notifications to the designated date/day/time window.
                if (!isValidTime) {
                    // Past due or not yet due (e.g., clock change): suppress.
                    alarmScheduler.cancelById(taskId)
                    return@launch
                }

                val serviceIntent = Intent(context, VoiceReminderService::class.java).apply {
                    putExtra("TASK_ID", taskId)
                    putExtra("TASK_TITLE", task.title.ifBlank { fallbackTitle })
                    putExtra("TASK_DESCRIPTION", task.description.ifBlank { fallbackDescription })
                }

                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        // Tolerate small delays/early triggers, but suppress far past-due notifications.
        private const val GRACE_BEFORE_SECONDS = 30L
        private const val GRACE_AFTER_SECONDS = 60L
    }
}
