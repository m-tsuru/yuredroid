package com.sasakulab.yure_android_client

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AccelerationGraph(
    sensorData: List<AccelerationData>,
    currentTime: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Sensor Data: ${sensorData.size} points | Last: ${if (sensorData.isNotEmpty()) "%.2f, %.2f, %.2f".format(sensorData[0].x, sensorData[0].y, sensorData[0].z) else "N/A"}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .border(1.dp, Color.Gray)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                // グラフパラメータ（JavaScriptコードと同様）
                val tDiv = 50f // 時間の縮小率
                val xMul = 10f // ゆれの拡大率（調整：100 -> 10）
                val xOff = 50f // X 方向のゆれの位置
                val yOff = 0f // Y 方向のゆれの位置
                val zOff = -50f // Z 方向のゆれの位置

                // 背景の横線を描画
                drawLine(
                    color = Color.Gray.copy(alpha = 0.25f),
                    start = Offset(0f, centerY + xOff),
                    end = Offset(width, centerY + xOff),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.25f),
                    start = Offset(0f, centerY + yOff),
                    end = Offset(width, centerY + yOff),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.25f),
                    start = Offset(0f, centerY + zOff),
                    end = Offset(width, centerY + zOff),
                    strokeWidth = 1f
                )

                // 縦線（時間軸のグリッド）を描画
                var i = 0f
                while (i < width) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.25f),
                        start = Offset(i, 0f),
                        end = Offset(i, height),
                        strokeWidth = 1f
                    )
                    i += 1000f / tDiv
                }

                if (sensorData.isEmpty()) return@Canvas

                // X軸のデータを赤色で描画
                val xPath = Path()
                var isFirstX = true
                sensorData.asReversed().forEach { data ->
                    val x = width - ((currentTime - data.t) / tDiv)
                    if (x >= 0 && x <= width) {
                        val y = (centerY + (data.x * xMul).toFloat() + xOff).coerceIn(0f, height)
                        if (isFirstX) {
                            xPath.moveTo(x, y)
                            isFirstX = false
                        } else {
                            xPath.lineTo(x, y)
                        }
                    }
                }
                drawPath(
                    path = xPath,
                    color = Color.Red,
                    style = Stroke(width = 2f)
                )

                // Y軸のデータを緑色で描画
                val yPath = Path()
                var isFirstY = true
                sensorData.asReversed().forEach { data ->
                    val x = width - ((currentTime - data.t) / tDiv)
                    if (x >= 0 && x <= width) {
                        val y = (centerY + (data.y * xMul).toFloat() + yOff).coerceIn(0f, height)
                        if (isFirstY) {
                            yPath.moveTo(x, y)
                            isFirstY = false
                        } else {
                            yPath.lineTo(x, y)
                        }
                    }
                }
                drawPath(
                    path = yPath,
                    color = Color.Green,
                    style = Stroke(width = 2f)
                )

                // Z軸のデータを青色で描画
                val zPath = Path()
                var isFirstZ = true
                sensorData.asReversed().forEach { data ->
                    val x = width - ((currentTime - data.t) / tDiv)
                    if (x >= 0 && x <= width) {
                        val y = (centerY + (data.z * xMul).toFloat() + zOff).coerceIn(0f, height)
                        if (isFirstZ) {
                            zPath.moveTo(x, y)
                            isFirstZ = false
                        } else {
                            zPath.lineTo(x, y)
                        }
                    }
                }
                drawPath(
                    path = zPath,
                    color = Color.Blue,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
