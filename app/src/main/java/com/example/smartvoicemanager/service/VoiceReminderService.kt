package com.example.smartvoicemanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.example.smartvoicemanager.data.prefs.SettingsManager
import com.example.smartvoicemanager.domain.repository.TaskRepository
import com.example.smartvoicemanager.domain.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VoiceReminderService : Service(), TextToSpeech.OnInitListener {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    
    private var taskId: Int = -1
    private var taskTitle: String = ""
    private var taskDescription: String = ""
    private var appLanguage: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var isTtsInitialized = false
    private var pendingSpeak = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val cycleRunnable = object : Runnable {
        override fun run() {
            if (isTtsInitialized && appLanguage != null) {
                speakTask()
            } else {
                pendingSpeak = true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_ACTION") {
            val stopTaskId = intent.getIntExtra("TASK_ID", -1)
            stopEverything()

            if (stopTaskId != -1) {
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    alarmScheduler.cancelById(stopTaskId)
                    val task = taskRepository.getTaskById(stopTaskId)
                    if (task != null) {
                        taskRepository.deleteTask(task)
                    }
                }
            }

            stopSelf()
            return START_NOT_STICKY
        }

        taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1
        taskTitle = intent?.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        taskDescription = intent?.getStringExtra("TASK_DESCRIPTION") ?: ""

        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            appLanguage = settingsManager.language.first()
            val customUri = settingsManager.defaultMusicUri.first()
            playAlarm(customUri)
            
            if (isTtsInitialized) {
                updateTtsSettings()
                if (pendingSpeak) {
                    pendingSpeak = false
                    handler.post(cycleRunnable)
                }
            }
            
            // Start the cycle: Ringtone for 5 seconds before first TTS
            handler.postDelayed(cycleRunnable, 5000)
        }

        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    handler.post {
                        mediaPlayer?.setVolume(0.1f, 0.1f)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        mediaPlayer?.setVolume(1.0f, 1.0f)
                        // Wait 10 seconds of ringtone before next TTS
                        handler.postDelayed(cycleRunnable, 10000)
                    }
                }

                override fun onError(utteranceId: String?) {
                    restoreVolumeAndSchedule()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    restoreVolumeAndSchedule()
                }

                private fun restoreVolumeAndSchedule() {
                    handler.post {
                        mediaPlayer?.setVolume(1.0f, 1.0f)
                        handler.postDelayed(cycleRunnable, 10000)
                    }
                }
            })
            
            isTtsInitialized = true
            if (appLanguage != null) {
                updateTtsSettings()
            }
            
            if (pendingSpeak) {
                pendingSpeak = false
                handler.post(cycleRunnable)
            }
        }
    }

    private fun updateTtsSettings() {
        val locale = if (appLanguage == "hi") Locale("hi", "IN") else Locale.US
        val result = tts?.setLanguage(locale)
        
        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val voices = tts?.voices
                val preferredVoice = if (appLanguage == "hi") {
                    voices?.find { it.locale.language == "hi" && it.locale.country == "IN" }
                } else {
                    voices?.find { it.locale.language == "en" && it.locale.country == "US" }
                }
                preferredVoice?.let { tts?.voice = it }
                
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.85f)
        }
    }

    private fun playAlarm(customUri: String?) {
        try {
            val alarmUri = if (customUri != null) {
                Uri.parse(customUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
                setVolume(1.0f, 1.0f)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (customUri != null) playAlarm(null)
        }
    }

    private fun speakTask() {
        val intro = if (appLanguage == "hi") {
            "नमस्ते, आपके लिए एक जरूरी सूचना है."
        } else {
            "Hello, you have an important reminder."
        }
        val textToSpeak = "$intro $taskTitle. $taskDescription"
        
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "TaskID_$taskId")
    }

    private fun stopEverything() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        vibrator?.cancel()
        
        handler.removeCallbacks(cycleRunnable)
        tts?.stop()
        pendingSpeak = false
    }

    private fun createNotification(): Notification {
        val channelId = "voice_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Voice Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, VoiceReminderService::class.java).apply {
            action = "STOP_ACTION"
            putExtra("TASK_ID", taskId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, taskId, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(taskTitle)
            .setContentText(taskDescription)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, 
                if (appLanguage == "hi") "स्टॉप (Stop)" else "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopEverything()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
