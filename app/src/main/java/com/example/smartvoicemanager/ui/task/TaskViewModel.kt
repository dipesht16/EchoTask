package com.example.smartvoicemanager.ui.task

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.usecase.CompleteTaskUseCase
import com.example.smartvoicemanager.domain.usecase.DeleteTaskUseCase
import com.example.smartvoicemanager.domain.usecase.GetActiveTasksUseCase
import com.example.smartvoicemanager.domain.usecase.ScheduleTaskUseCase
import com.example.smartvoicemanager.service.VoiceReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActiveTasksUseCase: GetActiveTasksUseCase,
    private val scheduleTaskUseCase: ScheduleTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = getActiveTasksUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(task: Task) {
        viewModelScope.launch {
            scheduleTaskUseCase(task)
        }
    }

    fun completeTask(id: Int) {
        viewModelScope.launch {
            completeTaskUseCase(id)
            // Stop notification if this task is being completed
            stopTaskServiceIfActive(id)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task)
            // Immediately stop the notification service if this task is deleted
            stopTaskServiceIfActive(task.id)
        }
    }

    private fun stopTaskServiceIfActive(taskId: Int) {
        val intent = Intent(context, VoiceReminderService::class.java).apply {
            action = "STOP_ACTION"
            putExtra("TASK_ID", taskId)
        }
        context.startService(intent)
    }
}
