package com.terusan.strongalarm.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.domain.model.MissionDifficulty
import com.terusan.strongalarm.domain.model.MissionType
import java.time.DayOfWeek

/**
 * アラーム作成・編集モーダルシート
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditSheet(
    initialAlarm: Alarm,
    onSave: (Alarm) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialAlarm.hour) }
    var minute by remember { mutableIntStateOf(initialAlarm.minute) }
    var label by remember { mutableStateOf(initialAlarm.label) }
    var selectedDays by remember { mutableStateOf(initialAlarm.daysOfWeek) }
    var volume by remember { mutableIntStateOf(initialAlarm.volume) }
    var isGradualVolume by remember { mutableStateOf(initialAlarm.isGradualVolume) }
    var snoozeInterval by remember { mutableIntStateOf(initialAlarm.snoozeIntervalMinutes) }
    var maxSnoozeCount by remember { mutableIntStateOf(initialAlarm.maxSnoozeCount) }
    var missionType by remember { mutableStateOf(initialAlarm.missionType) }
    var checkWakeUp by remember { mutableStateOf(initialAlarm.checkWakeUp) }
    var checkWakeUpMinutes by remember { mutableIntStateOf(initialAlarm.checkWakeUpAfterMinutes) }
    var isVibrate by remember { mutableStateOf(initialAlarm.isVibrate) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // タイトル行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialAlarm.id == 0L) "新しいアラーム" else "アラームを編集",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 時刻設定セクション
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 時
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("時", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("-") }
                        Text(
                            text = "%02d".format(hour),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                    }
                }

                Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))

                // 分
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { minute = (minute + 55) % 60 }) { Text("-5") }
                        Text(
                            text = "%02d".format(minute),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+5") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // アラーム名（ラベル）
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("アラーム名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 繰り返し曜日セレクタ
            Text("繰り返し曜日", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val weekDays = listOf(
                    DayOfWeek.MONDAY to "月",
                    DayOfWeek.TUESDAY to "火",
                    DayOfWeek.WEDNESDAY to "水",
                    DayOfWeek.THURSDAY to "木",
                    DayOfWeek.FRIDAY to "金",
                    DayOfWeek.SATURDAY to "土",
                    DayOfWeek.SUNDAY to "日"
                )
                weekDays.forEach { (day, label) ->
                    val isSelected = selectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedDays = if (isSelected) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 1. スヌーズ設定（回数上限指定！）
            Text("スヌーズ設定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("スヌーズ上限回数", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0, 1, 2, 3, 5).forEach { count ->
                        FilterChip(
                            selected = maxSnoozeCount == count,
                            onClick = { maxSnoozeCount = count },
                            label = { Text(if (count == 0) "なし" else "${count}回") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("スヌーズ間隔", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(3, 5, 10).forEach { mins ->
                        FilterChip(
                            selected = snoozeInterval == mins,
                            onClick = { snoozeInterval = mins },
                            label = { Text("${mins}分") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 2. 起床確認機能（二度寝防止！）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("起床確認（二度寝防止）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "アラーム停止後、指定時間後に本当に起きているか確認します。応答がなければ再鳴動します！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = checkWakeUp,
                    onCheckedChange = { checkWakeUp = it }
                )
            }

            if (checkWakeUp) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("確認までの時間", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5, 10, 15, 20).forEach { mins ->
                            FilterChip(
                                selected = checkWakeUpMinutes == mins,
                                onClick = { checkWakeUpMinutes = mins },
                                label = { Text("${mins}分後") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 3. アクション解除ミッション
            Text("解除ミッション", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "アラーム停止時に特定のアクションを要求します（近所迷惑防止のため、アクション中は一時消音されます）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MissionType.values().forEach { type ->
                    FilterChip(
                        selected = missionType == type,
                        onClick = { missionType = type },
                        label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 4. 音量設定 ＆ 徐々に音量アップ
            Text("音量 ＆ サウンド", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { volume = it.toInt() },
                    valueRange = 10f..100f,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${volume}%", style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("徐々に音量を大きくする", style = MaterialTheme.typography.bodyMedium)
                    Text("優しい音量から開始して驚かせません", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isGradualVolume, onCheckedChange = { isGradualVolume = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("バイブレーション", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isVibrate, onCheckedChange = { isVibrate = it })
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 保存ボタン
            Button(
                onClick = {
                    val updated = initialAlarm.copy(
                        hour = hour,
                        minute = minute,
                        label = label,
                        daysOfWeek = selectedDays,
                        volume = volume,
                        isGradualVolume = isGradualVolume,
                        snoozeIntervalMinutes = snoozeInterval,
                        maxSnoozeCount = maxSnoozeCount,
                        missionType = missionType,
                        checkWakeUp = checkWakeUp,
                        checkWakeUpAfterMinutes = checkWakeUpMinutes,
                        isVibrate = isVibrate
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("保存する", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
