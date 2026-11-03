package com.terusan.strongalarm.ui.main

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terusan.strongalarm.data.local.StrongAlarmDatabase
import com.terusan.strongalarm.data.repository.AlarmRepository
import com.terusan.strongalarm.data.repository.AlarmRepositoryImpl
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.service.AlarmReceiver
import com.terusan.strongalarm.service.AlarmRingingService
import com.terusan.strongalarm.service.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * メイン画面のUI状態
 */
data class MainUiState(
    val alarms: List<Alarm> = emptyList(),
    val editingAlarm: Alarm? = null,           // 編集中のアラーム（新規作成時は新規オブジェクト）
    val isEditSheetOpen: Boolean = false,      // 編集シートの表示状態
    val deletingAlarm: Alarm? = null           // 削除確認ダイアログ対象
)

/**
 * メイン画面用 ViewModel
 */
class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val scheduler: AlarmScheduler = AlarmScheduler(application)

    init {
        val db = StrongAlarmDatabase.getDatabase(application)
        repository = AlarmRepositoryImpl(db.alarmDao())
    }

    // DBのアラーム一覧を購読
    val alarms: StateFlow<List<Alarm>> = repository.alarmsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 鳴動中アラームのStateFlow (Serviceから取得)
    val ringingAlarm: StateFlow<Alarm?> = AlarmRingingService.ringingAlarm
    val isRingingMuted: StateFlow<Boolean> = AlarmRingingService.isMuted
    val missionResetTrigger: StateFlow<Int> = AlarmRingingService.missionResetTrigger

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * アラームのON/OFF切り替え
     */
    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm.id, isEnabled)
            val updated = alarm.copy(isEnabled = isEnabled, currentSnoozeCount = 0)
            if (isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    /**
     * 新規アラーム作成シートを開く
     */
    fun openNewAlarmSheet() {
        val newAlarm = Alarm(
            hour = 7,
            minute = 0,
            label = "アラーム",
            isEnabled = true
        )
        _uiState.value = _uiState.value.copy(
            editingAlarm = newAlarm,
            isEditSheetOpen = true
        )
    }

    /**
     * アラーム編集シートを開く
     */
    fun openEditAlarmSheet(alarm: Alarm) {
        _uiState.value = _uiState.value.copy(
            editingAlarm = alarm,
            isEditSheetOpen = true
        )
    }

    /**
     * 編集シートを閉じる
     */
    fun closeEditSheet() {
        _uiState.value = _uiState.value.copy(
            editingAlarm = null,
            isEditSheetOpen = false
        )
    }

    /**
     * アラームの保存
     */
    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val savedId = repository.saveAlarm(alarm)
            val finalAlarm = alarm.copy(id = if (alarm.id == 0L) savedId else alarm.id)

            if (finalAlarm.isEnabled) {
                scheduler.schedule(finalAlarm)
            } else {
                scheduler.cancel(finalAlarm)
            }
            closeEditSheet()
        }
    }

    /**
     * 削除確認ダイアログの表示
     */
    fun requestDelete(alarm: Alarm) {
        _uiState.value = _uiState.value.copy(deletingAlarm = alarm)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(deletingAlarm = null)
    }

    /**
     * アラームの削除実行
     */
    fun confirmDelete() {
        val alarm = _uiState.value.deletingAlarm ?: return
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
            dismissDeleteDialog()
        }
    }

    /**
     * 動作確認用: 今すぐテスト鳴動させる
     */
    fun testRing(alarm: Alarm) {
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
            putExtra(AlarmScheduler.EXTRA_IS_WAKEUP_CHECK, false)
        }
        context.sendBroadcast(intent)
    }

    /**
     * 鳴動中のアクション: ミッション中の消音
     */
    fun pauseSoundForMission() {
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_PAUSE_FOR_MISSION
        }
        context.startService(intent)
    }

    /**
     * 鳴動中のアクション: ユーザー操作による放置タイマー延長
     */
    fun notifyUserInteraction() {
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_USER_INTERACTION
        }
        context.startService(intent)
    }

    /**
     * 鳴動中のアクション: アラーム解除
     */
    fun dismissRinging() {
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_DISMISS
        }
        context.startService(intent)
    }

    /**
     * 鳴動中のアクション: スヌーズ
     */
    fun snoozeRinging() {
        val context = getApplication<Application>()
        val intent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_SNOOZE
        }
        context.startService(intent)
    }
}
