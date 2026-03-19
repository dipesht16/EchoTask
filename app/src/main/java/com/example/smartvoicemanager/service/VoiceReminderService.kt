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
import androidx.core.app.NotificationCompat
import com.example.smartvoicemanager.data.prefs.SettingsManager
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

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    
    private var taskId: Int = -1
    private var taskTitle: String = ""
    private var taskDescription: String = ""
    
    private val handler = Handler(Looper.getMainLooper())
    private var isTtsInitialized = false
    private var pendingSpeak = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val speakRunnable = object : Runnable {
        override fun run() {
            if (isTtsInitialized) {
                speakTask()
            }
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Initialize TTS immediately in onCreate to give it more warm-up time
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        taskId = intent?.getIntExtra("TASK_ID", -1) ?: -1
        taskTitle = intent?.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        taskDescription = intent?.getStringExtra("TASK_DESCRIPTION") ?: ""

        val action = intent?.action
        if (action == "STOP_ACTION") {
            stopEverything()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            val customUri = settingsManager.defaultMusicUri.first()
            playAlarm(customUri)
        }
        
        if (isTtsInitialized) {
            handler.post(speakRunnable)
        } else {
            pendingSpeak = true
        }

        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeHindi = Locale("hi", "IN")
            val result = tts?.setLanguage(localeHindi)
            
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val voices = tts?.voices
                    val indianVoice = voices?.find { it.locale.country == "IN" && it.locale.language == "hi" }
                        ?: voices?.find { it.locale.country == "IN" }
                    
                    indianVoice?.let { tts?.voice = it }
                }
                
                tts?.setPitch(0.9f)
                tts?.setSpeechRate(0.85f) // Slightly increased for better response
                isTtsInitialized = true
                
                if (pendingSpeak) {
                    pendingSpeak = false
                    handler.post(speakRunnable)
                }
            }
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
                setVolume(0.15f, 0.15f) // Lower alarm volume initially to emphasize TTS
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
        val textToSpeak = "नमस्ते, आपके लिए एक जरूरी सूचना है. $taskTitle. $taskDescription"
        
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        // Request immediate speech output
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "TaskID_$taskId")
    }

    private fun stopEverything() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        vibrator?.cancel()
        
        handler.removeCallbacks(speakRunnable)
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
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(taskTitle)
            .setContentText(taskDescription)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "स्टॉप (Stop)", stopPendingIntent)
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
