package com.example.smartvoicemanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smartvoicemanager.domain.model.Task
import com.example.smartvoicemanager.domain.model.TaskPriority
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val scheduledDateTime: String, // Stored as ISO string
    val priority: String,
    val isCompleted: Boolean,
    val isActive: Boolean
)

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        scheduledDateTime = LocalDateTime.parse(scheduledDateTime),
        priority = TaskPriority.valueOf(priority),
        isCompleted = isCompleted,
        isActive = isActive
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        scheduledDateTime = scheduledDateTime.toString(),
        priority = priority.name,
        isCompleted = isCompleted,
        isActive = isActive
    )
}
