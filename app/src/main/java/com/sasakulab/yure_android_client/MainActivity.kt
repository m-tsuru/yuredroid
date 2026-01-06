package com.sasakulab.yure_android_client

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
                        serverUrl = getServerUrl(),
                        bufferSize = getBufferSize(),
                        onStartClick = { startSharing() },
                        onStopClick = { stopSharing() },
                        onServerUrlChanged = { saveServerUrl(it) },
                        onBufferSizeChanged = { saveBufferSize(it) },
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

    // Get WebSocket Server URL
    private fun getServerUrl(): String {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        return prefs.getString("serverUrl", "wss://unstable.kusaremkn.com/yure") ?: "wss://unstable.kusaremkn.com/yure"
    }

    // Save WebSocket Server URL
    private fun saveServerUrl(url: String) {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("serverUrl", url).apply()
    }

    // Get Buffer Size
    private fun getBufferSize(): Int {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("bufferSize", 30)
    }

    // Save Buffer Size
    private fun saveBufferSize(size: Int) {
        val prefs = getSharedPreferences("yure_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("bufferSize", size).apply()
    }

    // Start to share accelerometer data
    private fun startSharing() {
        val serviceIntent = Intent(this, YureSensorService::class.java).apply {
            putExtra("serverUrl", getServerUrl())
            putExtra("bufferSize", getBufferSize())
        }
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
    serverUrl: String,
    bufferSize: Int,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onBufferSizeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showServerUrlDialog by remember { mutableStateOf(false) }
    var showBufferSizeDialog by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { showServerUrlDialog = true },
            enabled = !isSharing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Websocket Server URL")
        }

        Button(
            onClick = { showBufferSizeDialog = true },
            enabled = !isSharing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Buffer Size")
        }
    }

    if (showServerUrlDialog) {
        ServerUrlDialog(
            currentUrl = serverUrl,
            onDismiss = { showServerUrlDialog = false },
            onConfirm = { newUrl ->
                onServerUrlChanged(newUrl)
                showServerUrlDialog = false
            }
        )
    }

    if (showBufferSizeDialog) {
        BufferSizeDialog(
            currentSize = bufferSize,
            onDismiss = { showBufferSizeDialog = false },
            onConfirm = { newSize ->
                onBufferSizeChanged(newSize)
                showBufferSizeDialog = false
            }
        )
    }
}

@Composable
fun ServerUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebSocket Server URL") },
        text = {
            Column {
                Text("現在のURL: $currentUrl")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("wss://example.com/yure") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun BufferSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sizeText by remember { mutableStateOf(currentSize.toString()) }
    val size = sizeText.toIntOrNull() ?: currentSize

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buffer Size") },
        text = {
            Column {
                Text("現在のサイズ: $currentSize")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Buffer Size") },
                    placeholder = { Text("30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "バッファサイズは送信するデータの件数です",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(size) },
                enabled = sizeText.toIntOrNull() != null && size > 0
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
