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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.domain.model.MissionType
import com.terusan.strongalarm.ui.components.NumberWheelPicker
import com.terusan.strongalarm.ui.components.SettingWheelItem
import java.time.DayOfWeek

/**
 * アラーム作成・編集モーダルシート
 * 時刻・スヌーズ・起床確認をリール式スライドおよび直接数値入力で快適に設定可能
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

            // ==========================================
            // 1. 時刻設定セクション (リール状スライド ＆ 直接入力)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 時リール
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("時", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            NumberWheelPicker(
                                value = hour,
                                range = 0..23,
                                onValueChange = { hour = it },
                                format = { "%02d".format(it) }
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // 分リール (1分刻みでスライド＆直接入力)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            NumberWheelPicker(
                                value = minute,
                                range = 0..59,
                                onValueChange = { minute = it },
                                format = { "%02d".format(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "※上下にスライドして変更 / 数字タップで直接入力",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // アラーム名（ラベル）
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("アラーム名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                weekDays.forEach { (day, labelText) ->
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
                            text = labelText,
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

            // ==========================================
            // 2. スヌーズ設定 (スライド調整 ＆ 直接数値入力)
            // ==========================================
            Text("スヌーズ設定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            SettingWheelItem(
                title = "スヌーズ上限回数",
                value = maxSnoozeCount,
                range = 0..10,
                unitText = "回",
                zeroValueLabel = "なし（スヌーズ不可）",
                subtitle = "上限に達すると二度寝防止のためスヌーズ不可になります",
                onValueChange = { maxSnoozeCount = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingWheelItem(
                title = "スヌーズ間隔",
                value = snoozeInterval,
                range = 1..30,
                unitText = "分",
                onValueChange = { snoozeInterval = it }
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. 起床確認機能 (二度寝防止)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("起床確認（二度寝防止）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "アラーム停止後、指定時間後に本当に起きているか確認。応答がなければ再鳴動します！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = checkWakeUp,
                    onCheckedChange = { checkWakeUp = it }
                )
            }

            if (checkWakeUp) {
                Spacer(modifier = Modifier.height(10.dp))
                SettingWheelItem(
                    title = "確認までの時間",
                    value = checkWakeUpMinutes,
                    range = 1..60,
                    unitText = "分後",
                    subtitle = "アラームを止めてから何分後に確認するか設定します",
                    onValueChange = { checkWakeUpMinutes = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 4. アクション解除ミッション
            // ==========================================
            Text("解除ミッション", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "アラーム停止時に特定のアクションを要求します（近所迷惑防止のためアクション中は消音されます）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MissionType.values().forEach { type ->
                    FilterChip(
                        selected = missionType == type,
                        onClick = { missionType = type },
                        label = { Text(type.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 5. 音量設定 ＆ 徐々に音量アップ
            // ==========================================
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
                Column(modifier = Modifier.weight(1f)) {
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
