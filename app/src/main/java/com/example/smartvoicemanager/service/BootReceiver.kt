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
                tasks.forEach { task ->
                    alarmScheduler.schedule(task)
                }
            }
        }
    }
}
