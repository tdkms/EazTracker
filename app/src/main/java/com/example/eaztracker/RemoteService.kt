package com.example.eaztracker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RemoteService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val handler = Handler(Looper.getMainLooper())
    private var updateCount = 0

    private val runnable = object : Runnable {
        override fun run() {
            updateCount++
            val dataToSend = "Update number $updateCount"
            sendDataToServer(dataToSend)
            handler.postDelayed(this, 1000) // Repeat every second
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RemoteService", "Service started")
        createNotificationChannel()
        createNotification() // Create the foreground notification
        handler.post(runnable) // Start the periodic task
        return START_STICKY
    }


    private fun createNotification() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service Running")
            .setContentText("Sending data to server")
            .setSmallIcon(R.drawable.ic_launcher_background) // Replace with your icon
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("HardwareIds")
    private fun sendDataToServer(data: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val androidId = Settings.Secure.getString(
                    this@RemoteService.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                val encodedData = URLEncoder.encode(data, "UTF-8")
                val url =
                    URL("https://tdkms.s93.fun/EazTracker/SetUser.php?user=$androidId&data=data&value=$data")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                Log.d("RemoteService", "Response Code: $responseCode")
                connection.inputStream
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d("RemoteService", "TASK REMOVED")
    }


}
