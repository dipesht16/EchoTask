package com.example.smartvoicemanager.domain.model

import java.time.LocalDateTime

data class Task(
    val id: Int = 0,
    val title: String,
    val description: String,
    val scheduledDateTime: LocalDateTime,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}
