package com.example.eaztracker

import android.app.Application
import android.content.Intent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build


internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, RemoteService::class.java))
    }
}