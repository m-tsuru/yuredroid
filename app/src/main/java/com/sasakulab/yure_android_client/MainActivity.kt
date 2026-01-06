package com.sasakulab.yure_android_client

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sasakulab.yure_android_client.ui.theme.YureandroidclientTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var yureId: String
    private var isSharing = mutableStateOf(false)
    private var statusText = mutableStateOf("準備完了")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get or Generate ゆれ識別子
        yureId = getYureId()

        enableEdgeToEdge()
        setContent {
            YureandroidclientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    YureScreen(
                        yureId = yureId,
                        isSharing = isSharing.value,
                        statusText = statusText.value,
                        onStartClick = { startSharing() },
                        onStopClick = { stopSharing() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Generate ゆれ識別子
    private fun generateYureId(): String {
        val chars = "YUREyure"
        return (1..11).map { chars[Random.nextInt(8)] }.joinToString("")
    }

    // Get or generate / save ゆれ識別子
    private fun getYureId(): String {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("yureId", null)
        if (id == null) {
            id = generateYureId()
            prefs.edit().putString("yureId", id).apply()
        }
        return id
    }

    // Start to share accelerometer data
    private fun startSharing() {
        val serviceIntent = Intent(this, YureSensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        isSharing.value = true
        statusText.value = "バックグラウンドで送信中"
    }

    // Stop to share accelerometer data
    private fun stopSharing() {
        val serviceIntent = Intent(this, YureSensorService::class.java)
        stopService(serviceIntent)
        isSharing.value = false
        statusText.value = "停止しました"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

// Acceleration Data Class
data class AccelerationData(
    val yureId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val t: Long
)

@Composable
fun YureScreen(
    yureId: String,
    isSharing: Boolean,
    statusText: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "yureId",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = yureId,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartClick,
            enabled = !isSharing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStopClick,
            enabled = isSharing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop")
        }
    }
}
