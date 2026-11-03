package com.terusan.strongalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.util.AlarmTimeCalculator

/**
 * Androidの AlarmManager を操作して正確なアラームをスケジュールするクラス
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val TAG = "AlarmScheduler"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_IS_WAKEUP_CHECK = "extra_is_wakeup_check"

        // 起床確認用のリクエストコードオフセット（通常アラームIDと衝突させないため）
        private const val WAKEUP_CHECK_OFFSET = 1_000_000
    }

    /**
     * 通常のアラームをスケジュール
     */
    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val triggerTimeMillis = AlarmTimeCalculator.calculateNextTriggerTimeMillis(alarm)
        val pendingIntent = createAlarmPendingIntent(alarm.id, isSnooze = false, isWakeUpCheck = false)

        setExactAlarm(triggerTimeMillis, pendingIntent)
        Log.d(TAG, "アラーム(ID: ${alarm.id}) を ${alarm.formattedTime} にセットしました (trigger: $triggerTimeMillis)")
    }

    /**
     * スヌーズアラームをスケジュール
     */
    fun scheduleSnooze(alarm: Alarm) {
        val triggerTimeMillis = AlarmTimeCalculator.calculateSnoozeTriggerTimeMillis(alarm.snoozeIntervalMinutes)
        val pendingIntent = createAlarmPendingIntent(alarm.id, isSnooze = true, isWakeUpCheck = false)

        setExactAlarm(triggerTimeMillis, pendingIntent)
        Log.d(TAG, "スヌーズ(ID: ${alarm.id}) を ${alarm.snoozeIntervalMinutes}分後にセットしました")
    }

    /**
     * 起床確認アラームをスケジュール
     */
    fun scheduleWakeUpCheck(alarm: Alarm) {
        val triggerTimeMillis = AlarmTimeCalculator.calculateWakeUpCheckTriggerTimeMillis(alarm.checkWakeUpAfterMinutes)
        val pendingIntent = createAlarmPendingIntent(alarm.id, isSnooze = false, isWakeUpCheck = true)

        setExactAlarm(triggerTimeMillis, pendingIntent)
        Log.d(TAG, "起床確認(ID: ${alarm.id}) を ${alarm.checkWakeUpAfterMinutes}分後にセットしました")
    }

    /**
     * 起床確認アラームをキャンセル（ユーザーが起きたことを確認した場合）
     */
    fun cancelWakeUpCheck(alarmId: Long) {
        val pendingIntent = createAlarmPendingIntent(alarmId, isSnooze = false, isWakeUpCheck = true)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "起床確認(ID: $alarmId) をキャンセルしました")
    }

    /**
     * アラームをキャンセル
     */
    fun cancel(alarm: Alarm) {
        val pendingIntent = createAlarmPendingIntent(alarm.id, isSnooze = false, isWakeUpCheck = false)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        cancelWakeUpCheck(alarm.id)
        Log.d(TAG, "アラーム(ID: ${alarm.id}) をキャンセルしました")
    }

    /**
     * 正確なアラームを設定（Dozeモード貫通）
     */
    private fun setExactAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        // Android 12 (API 31) 以上でのExact Alarm権限確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact Alarmの権限がありません。setExactAndAllowWhileIdle は例外になる可能性があります")
            }
        }

        // ロック画面表示やDozeスリープを解除して鳴動させるため setAlarmClock または setExactAndAllowWhileIdle を使用
        val showIntent = Intent(context, com.terusan.strongalarm.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // AlarmClockInfo を使用すると、ステータスバーに次のアラームが表示され、OSの制限を最優先で回避できる
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    private fun createAlarmPendingIntent(
        alarmId: Long,
        isSnooze: Boolean,
        isWakeUpCheck: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
            putExtra(EXTRA_IS_WAKEUP_CHECK, isWakeUpCheck)
        }

        val requestCode = if (isWakeUpCheck) {
            (alarmId + WAKEUP_CHECK_OFFSET).toInt()
        } else {
            alarmId.toInt()
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
