package com.example.eaztracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class ServiceRestartReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.eaztracker.RESTART_SERVICE") {
            val serviceIntent = Intent(context, RemoteService::class.java)
            context.startForegroundService(serviceIntent) // Restart the service
        }
    }
}