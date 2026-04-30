package com.example.smartvoicemanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smartvoicemanager.domain.usecase.GetActiveTasksUseCase
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getActiveTasksUseCase: GetActiveTasksUseCase

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all active tasks on reboot
            CoroutineScope(Dispatchers.IO).launch {
                val tasks = getActiveTasksUseCase().first()
                val now = LocalDateTime.now()
                val graceAfterSeconds = 60L
                tasks.forEach { task ->
                    // If it's far past due, don't schedule an alarm that would trigger immediately.
                    if (task.scheduledDateTime.isBefore(now.minusSeconds(graceAfterSeconds))) {
                        return@forEach
                    }
                    alarmScheduler.schedule(task)
                }
            }
        }
    }
}
