package com.example.smartvoicemanager.domain.util

import com.example.smartvoicemanager.domain.model.Task

interface AlarmScheduler {
    fun schedule(task: Task)
    fun cancel(task: Task)
}
