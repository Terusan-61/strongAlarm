package com.terusan.strongalarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * リール状にスライドして数字を選択できるホイールピッカー
 * 中央タップで直接数値入力ダイアログも開くハイブリッド仕様
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { "%02d".format(it) },
    unitLabel: String = "",
    itemHeight: Dp = 48.dp
) {
    val items = remember(range) { range.toList() }
    val initialIndex = remember(value, range) {
        (value - range.first).coerceIn(0, items.size - 1)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    var showInputDialog by remember { mutableStateOf(false) }

    // 現在中央にあるアイテムのインデックスを計算
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0
            else {
                val centerOffset = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - centerOffset)
                }?.index ?: 0
            }
        }
    }

    // スクロール停止時に値を反映
    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) {
                    val newValue = items[index]
                    if (newValue != value) {
                        onValueChange(newValue)
                    }
                }
            }
    }

    // 外部から値が変更されたらスクロール位置を同期
    LaunchedEffect(value) {
        val targetIndex = (value - range.first).coerceIn(0, items.size - 1)
        if (targetIndex != centerIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)
            .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // 中央の選択ハイライト枠（タップで数値入力ダイアログ）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .clickable { showInputDialog = true }
        )

        // スライドリールリスト
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, itemValue ->
                val isSelected = index == centerIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            if (isSelected) {
                                showInputDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = format(itemValue),
                            fontSize = if (isSelected) 28.sp else 20.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.35f),
                            textAlign = TextAlign.Center
                        )
                        if (unitLabel.isNotBlank() && isSelected) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = unitLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // 直接数値入力用ダイアログ
    if (showInputDialog) {
        var inputString by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("数値を直接入力") },
            text = {
                OutlinedTextField(
                    value = inputString,
                    onValueChange = { inputString = it.filter { char -> char.isDigit() } },
                    label = { Text("範囲: ${range.first} 〜 ${range.last}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputString.toIntOrNull()
                        if (parsed != null && parsed in range) {
                            onValueChange(parsed)
                        }
                        showInputDialog = false
                    }
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
