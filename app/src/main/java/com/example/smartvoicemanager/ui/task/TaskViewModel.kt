package com.example.smartvoicemanager.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.usecase.CompleteTaskUseCase
import com.example.smartvoicemanager.domain.usecase.DeleteTaskUseCase
import com.example.smartvoicemanager.domain.usecase.GetActiveTasksUseCase
import com.example.smartvoicemanager.domain.usecase.ScheduleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
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
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deleteTaskUseCase(task)
        }
    }
}
