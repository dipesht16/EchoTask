package com.example.smartvoicemanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartVoiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}