package com.sasakulab.yure_android_client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import okhttp3.*

class YureSensorService : Service(), SensorEventListener {

    private var bufferSize: Int = 30
    private var hostName: String = "wss://unstable.kusaremkn.com/yure"
    private val CHANNEL_ID = "YureSensorServiceChannel"
    private val NOTIFICATION_ID = 1

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var webSocket: WebSocket? = null
    private val dataBuffer = mutableListOf<AccelerationData>()
    private val gson = Gson()
    private lateinit var yureId: String
    private val bufferLock = Any()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        // Get ゆれ識別子
        yureId = getYureId()

        // Create notification channel
        createNotificationChannel()

        // Initialize Sensor Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Acquire WakeLock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "YureSensorService::WakeLock"
        ).apply {
            acquire(10*60*60*1000L /*10 hours*/)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get settings from intent
        intent?.let {
            hostName = it.getStringExtra("serverUrl") ?: hostName
            bufferSize = it.getIntExtra("bufferSize", bufferSize)
        }

        // Connect WebSocket with configured URL
        connectWebSocket()

        // Start foreground service with notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start sensor listening
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getYureId(): String {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        return prefs.getString("yureId", null) ?: "unknown"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yure Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "加速度センサーデータを取得・送信しています"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yure Sensor")
            .setContentText("加速度データを送信中...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(hostName)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                updateNotification("Connected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                updateNotification("Connection Error")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION || it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val data = AccelerationData(
                    yureId = yureId,
                    x = it.values[0].toDouble(),
                    y = it.values[1].toDouble(),
                    z = it.values[2].toDouble(),
                    t = System.currentTimeMillis(),
                    userAgent = String.format("yuredroid %s on %s %s", packageManager.getPackageInfo(packageName, 0).versionName, Build.MANUFACTURER, Build.MODEL),
                )

                // Send data to MainActivity via listener
                MainActivity.dataListener?.invoke(data)

                synchronized(bufferLock) {
                    dataBuffer.add(data)
                    if (dataBuffer.size >= bufferSize) {
                        sendDataToServer()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do Nothing
    }

    private fun sendDataToServer() {
        val dataToSend: List<AccelerationData>
        synchronized(bufferLock) {
            if (dataBuffer.isEmpty()) return
            dataToSend = dataBuffer.toList()
            dataBuffer.clear()
        }

        val json = gson.toJson(dataToSend)
        val sent = webSocket?.send(json) ?: false

        if (sent) {
            updateNotification("送信中: ${dataToSend.size}件")
        } else {
            updateNotification("送信失敗")
        }
    }

    private fun updateNotification(status: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yure Sensor")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)

        // Send remaining data
        synchronized(bufferLock) {
            if (dataBuffer.isNotEmpty()) {
                sendDataToServer()
            }
        }

        webSocket?.close(1000, "Service destroyed")

        // Release WakeLock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart service when task is removed
        val restartServiceIntent = Intent(applicationContext, YureSensorService::class.java).apply {
            setPackage(packageName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }
    }
}
