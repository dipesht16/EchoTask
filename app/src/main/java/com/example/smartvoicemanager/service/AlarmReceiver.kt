package com.example.smartvoicemanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val description = intent.getStringExtra("TASK_DESCRIPTION") ?: ""

        if (taskId != -1) {
            val serviceIntent = Intent(context, VoiceReminderService::class.java).apply {
                putExtra("TASK_ID", taskId)
                putExtra("TASK_TITLE", title)
                putExtra("TASK_DESCRIPTION", description)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
