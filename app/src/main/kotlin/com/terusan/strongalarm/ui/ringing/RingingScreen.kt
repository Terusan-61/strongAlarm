package com.terusan.strongalarm.ui.ringing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.domain.model.MissionType
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * アラーム鳴動中の全画面オーバーレイUI
 */
@Composable
fun RingingScreen(
    alarm: Alarm,
    isMuted: Boolean,
    missionResetTrigger: Int,
    onStartMission: () -> Unit,
    onUserInteraction: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMissionActive by remember { mutableStateOf(false) }

    // サービス側から放置リセット通知（missionResetTrigger変化）が来たらミッション画面を閉じる
    LaunchedEffect(missionResetTrigger) {
        if (missionResetTrigger > 0) {
            isMissionActive = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isMissionActive && alarm.missionType != MissionType.NONE) {
                // ミッション実行中画面
                MissionContainer(
                    missionType = alarm.missionType,
                    onUserInteraction = onUserInteraction,
                    onMissionCleared = onDismiss
                )
            } else {
                // アラーム鳴動メイン画面
                AlarmRingingContent(
                    alarm = alarm,
                    isMuted = isMuted,
                    hasSnoozeLeft = alarm.currentSnoozeCount < alarm.maxSnoozeCount,
                    onStartMission = {
                        isMissionActive = true
                        onStartMission()
                    },
                    onDismissDirect = onDismiss,
                    onSnooze = onSnooze
                )
            }
        }
    }
}

/**
 * 鳴動中メイン画面
 */
@Composable
private fun AlarmRingingContent(
    alarm: Alarm,
    isMuted: Boolean,
    hasSnoozeLeft: Boolean,
    onStartMission: () -> Unit,
    onDismissDirect: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 消音中インジケータ（近所迷惑防止中）
        if (isMuted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.VolumeMute, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("アクション中につき一時消音中（放置すると再鳴動します）", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = alarm.formattedTime,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = alarm.label.ifBlank { "アラーム" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // スヌーズ情報
        if (alarm.maxSnoozeCount > 0) {
            Text(
                text = "スヌーズ回数: ${alarm.currentSnoozeCount} / ${alarm.maxSnoozeCount}回",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasSnoozeLeft) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 停止 / ミッション開始ボタン
        if (alarm.missionType == MissionType.NONE) {
            Button(
                onClick = onDismissDirect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("アラームを停止", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onStartMission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ミッションを開始して停止", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // スヌーズボタン
        if (alarm.maxSnoozeCount > 0) {
            OutlinedButton(
                onClick = onSnooze,
                enabled = hasSnoozeLeft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (hasSnoozeLeft) "スヌーズ (${alarm.snoozeIntervalMinutes}分後)" else "スヌーズ上限到達",
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * ミッションコンテナ
 */
@Composable
private fun MissionContainer(
    missionType: MissionType,
    onUserInteraction: () -> Unit,
    onMissionCleared: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "解除ミッション: ${missionType.displayName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "※操作中は消音されています。無操作が続くと再鳴動します！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (missionType) {
                MissionType.MATH -> MathMissionView(onUserInteraction, onMissionCleared)
                MissionType.SHAKE -> ShakeMissionView(onUserInteraction, onMissionCleared)
                MissionType.BARCODE -> BarcodeMissionView(onUserInteraction, onMissionCleared)
                MissionType.NONE -> onMissionCleared()
            }
        }
    }
}

/**
 * 1. 計算問題ミッション
 */
@Composable
private fun MathMissionView(
    onUserInteraction: () -> Unit,
    onCleared: () -> Unit
) {
    var num1 by remember { mutableIntStateOf(Random.nextInt(12, 45)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(11, 45)) }
    var input by remember { mutableStateOf("") }
    var clearedCount by remember { mutableIntStateOf(0) }
    val targetClears = 3

    val correctAnswer = num1 + num2

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("問題 ($clearedCount / $targetClears 問正解)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$num1 + $num2 = ?",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = input.ifBlank { "回答を入力" },
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (input.isBlank()) Color.Gray else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 数字テンキー
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6),
                listOf(7, 8, 9)
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { n ->
                        FilledTonalButton(
                            onClick = {
                                onUserInteraction()
                                if (input.length < 4) input += n
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text("$n", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        onUserInteraction()
                        input = ""
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Text("C", fontSize = 18.sp)
                }
                FilledTonalButton(
                    onClick = {
                        onUserInteraction()
                        if (input.length < 4) input += "0"
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Text("0", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onUserInteraction()
                        if (input.toIntOrNull() == correctAnswer) {
                            val nextCount = clearedCount + 1
                            clearedCount = nextCount
                            input = ""
                            if (nextCount >= targetClears) {
                                onCleared()
                            } else {
                                num1 = Random.nextInt(12, 49)
                                num2 = Random.nextInt(11, 49)
                            }
                        } else {
                            input = ""
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "OK")
                }
            }
        }
    }
}

/**
 * 2. 端末シェイクミッション
 */
@Composable
private fun ShakeMissionView(
    onUserInteraction: () -> Unit,
    onCleared: () -> Unit
) {
    val context = LocalContext.current
    var shakeCount by remember { mutableIntStateOf(0) }
    val targetShakes = 25

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastShakeTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
                if (acceleration > 8.0) { // シェイク閾値
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTime > 200) {
                        lastShakeTime = now
                        shakeCount++
                        onUserInteraction()

                        if (shakeCount >= targetShakes) {
                            onCleared()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("端末を素早く振ってください！", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("$shakeCount / $targetShakes 回", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (shakeCount.toFloat() / targetShakes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp)
        )
    }
}

/**
 * 3. バーコード/QRスキャンミッション
 */
@Composable
private fun BarcodeMissionView(
    onUserInteraction: () -> Unit,
    onCleared: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "洗面所やキッチンのバーコードをスキャンして起床を証明してください！",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onUserInteraction()
                onCleared()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("スキャン完了（テスト確認）")
        }
    }
}
