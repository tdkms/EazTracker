package com.example.eaztracker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RemoteService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val handler = Handler(Looper.getMainLooper())
    private var updateCount = 0

    private val runnable = object : Runnable {
        override fun run() {
            updateCount++
            sendDataToServer()
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

    private fun sendDataToServer(isConnected: Int = 1) {
        val pvIp = getPvIpAddress()
        val pubIp = getPubIpAddress()
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val batteryLevel = getBatteryLevel(this)
        val deviceModel = Build.MODEL
        val brand = Build.BRAND
        val manufacturer = Build.MANUFACTURER
        val screenResolution = getScreenResolution(this)
        val dpi = getDpi(this)
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val totalRam = getTotalRam(this)
        val sensors = getAllSensors(this)
        val isCharging = 0
        val accounts = ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val androidId = Settings.Secure.getString(
                    this@RemoteService.contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                val time = System.currentTimeMillis() / 1000
                val ping = getConnectionSpeed()

                // URL encode all parameters to ensure they are safe for URLs
                val encodedUser = URLEncoder.encode(androidId, StandardCharsets.UTF_8.toString())
                val encodedTime = URLEncoder.encode(time.toString(), StandardCharsets.UTF_8.toString())
                val encodedIsConnected = URLEncoder.encode(isConnected.toString(), StandardCharsets.UTF_8.toString())
                val encodedPing = URLEncoder.encode(ping, StandardCharsets.UTF_8.toString())
                val encodedDevice = URLEncoder.encode(deviceModel, StandardCharsets.UTF_8.toString())
                val encodedPvIp = URLEncoder.encode(pvIp, StandardCharsets.UTF_8.toString())
                val encodedPubIp = URLEncoder.encode(pubIp, StandardCharsets.UTF_8.toString())
                val encodedAndroidVersion = URLEncoder.encode(androidVersion, StandardCharsets.UTF_8.toString())
                val encodedApiLevel = URLEncoder.encode(apiLevel.toString(), StandardCharsets.UTF_8.toString())
                val encodedBatteryLevel = URLEncoder.encode(batteryLevel.toString(), StandardCharsets.UTF_8.toString())
                val encodedBrand = URLEncoder.encode(brand, StandardCharsets.UTF_8.toString())
                val encodedManufacturer = URLEncoder.encode(manufacturer, StandardCharsets.UTF_8.toString())
                val encodedScreenResolution = URLEncoder.encode(screenResolution, StandardCharsets.UTF_8.toString())
                val encodedDpi = URLEncoder.encode(dpi.toString(), StandardCharsets.UTF_8.toString())
                val encodedAvailableProcessors = URLEncoder.encode(availableProcessors.toString(), StandardCharsets.UTF_8.toString())
                val encodedTotalRam = URLEncoder.encode(totalRam.toString(), StandardCharsets.UTF_8.toString())
                val encodedSensors = URLEncoder.encode(sensors.joinToString(","), StandardCharsets.UTF_8.toString())
                val encodedIsCharging = URLEncoder.encode(isCharging.toString(), StandardCharsets.UTF_8.toString())
                val encodedAccounts = ""
                val encodedBatteryPercentage = URLEncoder.encode(batteryLevel.toString(), StandardCharsets.UTF_8.toString())

                // Build the URL for GET request
                val baseUrl = "https://rigeltube.ir/rigelbtrigelcode/SupportBot/Telegram/EazTracker/SetUser.php"
                val fullUrl = "$baseUrl?" +
                        "user=$encodedUser" +
                        "&time=$encodedTime" +
                        "&isConnected=$encodedIsConnected" +
                        "&ping=$encodedPing" +
                        "&device=$encodedDevice" +
                        "&pvIp=$encodedPvIp" +
                        "&pubIp=$encodedPubIp" +
                        "&androidVersion=$encodedAndroidVersion" +
                        "&apiLevel=$encodedApiLevel" +
                        "&batteryLevel=$encodedBatteryLevel" +
                        "&deviceModel=$encodedDevice" +
                        "&brand=$encodedBrand" +
                        "&manufacturer=$encodedManufacturer" +
                        "&screenResolution=$encodedScreenResolution" +
                        "&dpi=$encodedDpi" +
                        "&availableProcessors=$encodedAvailableProcessors" +
                        "&totalRam=$encodedTotalRam" +
                        "&sensors=$encodedSensors" +
                        "&isCharging=$encodedIsCharging" +
                        "&accounts=$encodedAccounts" +
                        "&batteryPercentage=$encodedBatteryPercentage"

                // Create the URL object for GET request
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection

                // Set the request properties (GET method)
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:90.0) Gecko/90.0 Firefox/90.0")
                connection.setRequestProperty("Accept", "application/json")

                // Get the response code and handle the response
                val responseCode = connection.responseCode
                Log.d("RemoteService", "Request URL: $fullUrl")
                Log.d("RemoteService", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().readText()
                    Log.d("RemoteService", "Response: $response")
                } else {
                    Log.e("RemoteService", "Failed to send data: Response Code $responseCode")
                }
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
        sendDataToServer(0)
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d("RemoteService", "TASK REMOVED")
        sendDataToServer(0)
    }


    fun getAllSensors(context: Context): List<Sensor> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
    }


    fun getTotalRam(context: Context): Long {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024) // Convert bytes to GB
    }

    fun getScreenResolution(context: Context): String {
        val metrics = context.resources.displayMetrics
        return "${metrics.widthPixels} x ${metrics.heightPixels}"
    }

    fun getDpi(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (metrics.densityDpi)
    }


    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    suspend fun getConnectionSpeed(): String {
        var pingTime: Long? = null

        // Perform a ping to check the latency
        withContext(Dispatchers.IO) {
            pingTime = ping("https://www.google.com") // Using a reliable server for ping
        }

        return when {
            pingTime == null -> "No connection"
            pingTime!! <= 50 -> "Great (Ping: $pingTime ms)"
            pingTime!!.toInt() in 51..150 -> "Good (Ping: $pingTime ms)"
            pingTime!!.toInt() in 151..300 -> "Weak (Ping: $pingTime ms)"
            else -> "Very Weak (Ping: $pingTime ms)"
        }
    }

    // Function to ping a server and measure the response time
    suspend fun ping(url: String): Long? {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            return@withContext try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "HEAD"
                    connectTimeout = 1000 // Timeout in milliseconds
                    readTimeout = 1000
                }
                connection.connect()
                val endTime = System.currentTimeMillis()
                endTime - startTime // Ping time in milliseconds
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getBatteryInfo(context: Context): Intent? {
        val batteryIntent = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, batteryIntent)
    }

    fun getBatteryLevel(context: Context): Int {
        val batteryIntent = getBatteryInfo(context)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level / scale.toFloat() * 100).toInt()
    }

    fun getPvIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in interfaces) {
                val addresses = intf.inetAddresses.toList()
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun getPubIpAddress(): String? {
        return try {
            // Enable policy to allow network operations on the main thread (not recommended for production)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            // Fetch public IP address from ipify API
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection()
            val input = BufferedReader(InputStreamReader(connection.getInputStream()))

            input.readLine()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}
