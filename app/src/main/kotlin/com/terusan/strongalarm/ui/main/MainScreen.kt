package com.terusan.strongalarm.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terusan.strongalarm.ui.edit.AlarmEditSheet
import com.terusan.strongalarm.ui.ringing.RingingScreen

/**
 * アプリのメイン画面（アラーム一覧・作成・編集・鳴動管理）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ringingAlarm by viewModel.ringingAlarm.collectAsStateWithLifecycle()
    val isRingingMuted by viewModel.isRingingMuted.collectAsStateWithLifecycle()
    val missionResetTrigger by viewModel.missionResetTrigger.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "StrongAlarm",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openNewAlarmSheet() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "アラームを追加"
                    )
                }
            }
        ) { innerPadding ->
            if (alarms.isEmpty()) {
                // アラーム未登録時のガイド表示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "アラームがまだ設定されていません",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "右下の「＋」ボタンを押して、絶対に起きられるアラームを設定しましょう！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // アラーム一覧リスト
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                            onClick = { viewModel.openEditAlarmSheet(alarm) },
                            onDelete = { viewModel.requestDelete(alarm) },
                            onTestRing = { viewModel.testRing(alarm) }
                        )
                    }
                }
            }
        }

        // 編集モーダルシート
        if (uiState.isEditSheetOpen && uiState.editingAlarm != null) {
            AlarmEditSheet(
                initialAlarm = uiState.editingAlarm!!,
                onSave = { updated -> viewModel.saveAlarm(updated) },
                onDismiss = { viewModel.closeEditSheet() }
            )
        }

        // 削除確認ダイアログ
        uiState.deletingAlarm?.let { alarm ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text("アラームの削除") },
                text = { Text("「${alarm.label.ifBlank { alarm.formattedTime }}」を削除しますか？") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                        Text("キャンセル")
                    }
                }
            )
        }

        // 鳴動画面のフルスクリーンオーバーレイ（アラーム鳴動中に表示）
        AnimatedVisibility(
            visible = ringingAlarm != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ringingAlarm?.let { activeAlarm ->
                RingingScreen(
                    alarm = activeAlarm,
                    isMuted = isRingingMuted,
                    missionResetTrigger = missionResetTrigger,
                    onStartMission = { viewModel.pauseSoundForMission() },
                    onUserInteraction = { viewModel.notifyUserInteraction() },
                    onDismiss = { viewModel.dismissRinging() },
                    onSnooze = { viewModel.snoozeRinging() }
                )
            }
        }
    }
}
