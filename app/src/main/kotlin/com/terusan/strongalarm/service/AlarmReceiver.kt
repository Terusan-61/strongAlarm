package com.terusan.strongalarm.service

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.terusan.strongalarm.MainActivity
import com.terusan.strongalarm.R
import com.terusan.strongalarm.ui.wakeup.WakeUpCheckActivity

/**
 * AlarmManager の時間到達時にトリガーされるレシーバー
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val WAKEUP_CHECK_CHANNEL_ID = "strong_alarm_wakeup_check_channel"
        const val WAKEUP_CHECK_NOTIFICATION_ID = 8888
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        val isWakeUpCheck = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_WAKEUP_CHECK, false)

        Log.d(TAG, "アラーム受信! ID: $alarmId, スヌーズ: $isSnooze, 起床確認: $isWakeUpCheck")

        if (alarmId == -1L) return

        // 画面が消灯している場合、物理的にディスプレイを強制点灯させる WakeLock を取得
        acquireWakeLockToTurnScreenOn(context)

        if (isWakeUpCheck) {
            // ==========================================
            // 起床確認フェーズ
            // FullScreenIntent を設定した高優先度通知を発行して、
            // Android 14+ の BAL（Background Activity Launch）制限を突破し画面を強制点灯＆フルスクリーン起動
            // ==========================================
            val checkIntent = Intent(context, WakeUpCheckActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(WakeUpCheckActivity.EXTRA_ALARM_ID, alarmId)
            }

            val activityOptionsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                ).toBundle()
            } else null

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (alarmId + 10000).toInt(),
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                activityOptionsBundle
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 起床確認用の通知チャンネルを作成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    WAKEUP_CHECK_CHANNEL_ID,
                    "起床確認通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "起床確認時に画面を点灯させて確認画面を表示する通知です"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, WAKEUP_CHECK_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("起床確認（二度寝防止）")
                .setContentText("起きていますか？タップして確認してください")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(WAKEUP_CHECK_NOTIFICATION_ID, notification)

            // 直接の起動も念のため試行
            try {
                context.startActivity(checkIntent, activityOptionsBundle)
            } catch (e: Exception) {
                Log.w(TAG, "直接の startActivity は制限されました（FullScreenIntentで起動されます）", e)
            }
        } else {
            // ==========================================
            // 通常アラーム鳴動フェーズ
            // 大音量アラームサービス + 鳴動画面を起動
            // ==========================================
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_START_RINGING
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingService.EXTRA_IS_WAKEUP_CHECK, false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmRingingService.EXTRA_IS_WAKEUP_CHECK, false)
            }
            context.startActivity(activityIntent)
        }
    }

    /**
     * スリープ状態（画面消灯）から物理的にディスプレイをパッと点灯させる
     */
    @Suppress("DEPRECATION")
    private fun acquireWakeLockToTurnScreenOn(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "StrongAlarm:WakeScreenLock"
            )
            wakeLock.acquire(10_000L) // 10秒間画面点灯を強制維持
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock の取得に失敗しました", e)
        }
    }
}
