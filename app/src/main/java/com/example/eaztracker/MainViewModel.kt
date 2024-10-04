package com.example.eaztracker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eaztracker.Constants.REGISTER_URL
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
import java.net.SocketTimeoutException
import java.net.URL

class MainViewModel : ViewModel() {

    var state by mutableStateOf(MainState())

    @SuppressLint("HardwareIds")
    fun checkRegistration(
        context: Context,
        isCharging: Boolean,
        batteryCapacity: Int? = 100,
        batteryCycle: Int?
    ) {
        val pvIp = getPvIpAddress()
        val pubIp = getPubIpAddress()
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val batteryLevel = getBatteryLevel(context)
        val isConnected = isInternetConnected(context)
        val deviceModel = Build.MODEL
        val brand = Build.BRAND
        val manufacturer = Build.MANUFACTURER
        val screenResolution = getScreenResolution(context)
        val dpi = getDpi(context)
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val totalRam = getTotalRam(context)
        val sensors = getAllSensors(context)

        viewModelScope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val timestamp: Long = System.currentTimeMillis()
                val connectionSpeed = viewModelScope.async {
                    getConnectionSpeed()
                }.await()

                Log.d("TAG", "androidId: $androidId, isCharging: $isCharging, batteryCapacity: $batteryCapacity")

                val urlQuery = "https://rigeltube.ir/rigelbtrigelcode/SupportBot/Telegram/EazTracker/GetUser.php?user=${androidId.toString()}&isCharging=${isCharging.toString()}&batteryCapacity=${batteryCapacity.toString()}"


                Log.d("TAG", "Request URL: $urlQuery")


                val url = URL(urlQuery)
                connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val responseBody = inputStream.bufferedReader().use { it.readText() }

                    Log.d("TAG", "RESULT IS: $responseBody")
                } else {
                    Log.e("TAG", "Unexpected HTTP response: $responseCode")
                }

            } catch (e: SocketTimeoutException) {
                Log.e("TAG", "Request timed out: ${e.message}")
            } catch (e: IOException) {
                Log.e("TAG", "Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("TAG", "Unexpected error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
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