package com.terusan.strongalarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * スヌーズ上限や間隔などを「リール・スライダー・直接数値入力」で設定できる汎用行コンポーネント
 */
@Composable
fun SettingWheelItem(
    title: String,
    value: Int,
    range: IntRange,
    unitText: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    zeroValueLabel: String? = null, // 0の時に「なし」と表示する場合など
    subtitle: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 上段: タイトルと現在の値（タップで直接入力）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 現在の値バッジ（タップで直接入力可能）
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showDialog = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = if (value == 0 && zeroValueLabel != null) {
                        zeroValueLabel
                    } else {
                        "$value $unitText"
                    }
                    Text(
                        text = displayText,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 下段: スライダー & ステッパーで直接スライド・微調整
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (value - 1).coerceAtLeast(range.first)
                        onValueChange(next)
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("-")
                }

                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.toInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = {
                        val next = (value + 1).coerceAtMost(range.last)
                        onValueChange(next)
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+")
                }
            }
        }
    }

    // 直接数値入力ダイアログ
    if (showDialog) {
        var inputString by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("$title の設定") },
            text = {
                Column {
                    Text("数値を直接入力してください (${range.first} 〜 ${range.last} $unitText)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputString,
                        onValueChange = { inputString = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputString.toIntOrNull()
                        if (parsed != null && parsed in range) {
                            onValueChange(parsed)
                        }
                        showDialog = false
                    }
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
