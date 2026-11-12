package com.terusan.strongalarm.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.terusan.strongalarm.MainActivity
import com.terusan.strongalarm.R
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.util.AlarmTimeCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        const val EXTRA_CHECK_WAKEUP_SILENT = "extra_check_wakeup_silent"

        // 起床確認用のリクエストコードオフセット（通常アラームIDと衝突させないため）
        private const val WAKEUP_CHECK_OFFSET = 1_000_000

        // 起床確認の事前予告通知チャンネル＆通知ID
        const val WAKEUP_CHECK_UPCOMING_CHANNEL_ID = "strong_alarm_wakeup_check_upcoming_channel"
        private const val BASE_UPCOMING_NOTIFICATION_ID = 90_000

        fun getUpcomingNotificationId(alarmId: Long): Int = (BASE_UPCOMING_NOTIFICATION_ID + alarmId).toInt()
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
        val pendingIntent = createAlarmPendingIntent(
            alarmId = alarm.id,
            isSnooze = false,
            isWakeUpCheck = true,
            checkWakeUpSilent = alarm.checkWakeUpSilent
        )

        setExactAlarm(triggerTimeMillis, pendingIntent)
        Log.d(TAG, "起床確認(ID: ${alarm.id}) を ${alarm.checkWakeUpAfterMinutes}分後にセットしました (サイレント: ${alarm.checkWakeUpSilent})")

        // 起床確認が控えていることをユーザーに知らせる事前予告通知を表示
        showWakeUpCheckUpcomingNotification(alarm, triggerTimeMillis)
    }

    /**
     * 起床確認アラームをキャンセル（ユーザーが起きたことを確認した場合）
     */
    fun cancelWakeUpCheck(alarmId: Long) {
        val pendingIntent = createAlarmPendingIntent(alarmId, isSnooze = false, isWakeUpCheck = true)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        cancelWakeUpCheckUpcomingNotification(alarmId)
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
     * 起床確認の事前予告通知を表示
     * 例:「10分後 (01:23) に起床確認を行います。画面に表示されるボタンを60秒以内に押さなければ、アラームが再鳴動しますのでご注意ください！」
     */
    fun showWakeUpCheckUpcomingNotification(alarm: Alarm, triggerTimeMillis: Long) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 通知チャンネル作成（静音・低優先度・ロック画面表示可）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    WAKEUP_CHECK_UPCOMING_CHANNEL_ID,
                    "起床確認の事前予告",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "起床確認が控えていることを事前にお知らせする通知です"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(triggerTimeMillis))
            val minutes = alarm.checkWakeUpAfterMinutes

            val title = "⏰ 起床確認が控えています（${minutes}分後）"
            val message = "${minutes}分後 ($formattedTime) に起床確認を行います。画面に表示されるボタンを60秒以内に押さなければ、アラームが再鳴動しますのでご注意ください！"

            // タップ時は MainActivity を開く（事前解除ボタンは配置せず、あえてチェックを受けさせる方針）
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                getUpcomingNotificationId(alarm.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, WAKEUP_CHECK_UPCOMING_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true) // 誤ってスワイプで消してしまわないように常駐
                .setContentIntent(pendingIntent)
                .setShowWhen(true)
                .setWhen(triggerTimeMillis)
                .build()

            notificationManager.notify(getUpcomingNotificationId(alarm.id), notification)
            Log.d(TAG, "起床確認の事前予告通知を表示しました: ID=${getUpcomingNotificationId(alarm.id)}, 予定時刻=$formattedTime")
        } catch (e: Exception) {
            Log.e(TAG, "起床確認の事前予告通知の表示に失敗しました", e)
        }
    }

    /**
     * 起床確認の事前予告通知を消去
     */
    fun cancelWakeUpCheckUpcomingNotification(alarmId: Long) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(getUpcomingNotificationId(alarmId))
            Log.d(TAG, "起床確認の事前予告通知を消去しました: ID=${getUpcomingNotificationId(alarmId)}")
        } catch (e: Exception) {
            Log.e(TAG, "起床確認の事前予告通知の消去に失敗しました", e)
        }
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
        isWakeUpCheck: Boolean,
        checkWakeUpSilent: Boolean = true
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
            putExtra(EXTRA_IS_WAKEUP_CHECK, isWakeUpCheck)
            putExtra(EXTRA_CHECK_WAKEUP_SILENT, checkWakeUpSilent)
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
