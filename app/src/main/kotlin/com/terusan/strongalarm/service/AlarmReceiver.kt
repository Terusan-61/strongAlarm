package com.terusan.strongalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.terusan.strongalarm.MainActivity
import com.terusan.strongalarm.ui.wakeup.WakeUpCheckActivity

/**
 * AlarmManager の時間到達時にトリガーされるレシーバー
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
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
            // 画面を強制点灯し、WakeUpCheckActivity（60秒確認）を全画面起動
            // ==========================================
            val checkIntent = Intent(context, WakeUpCheckActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(WakeUpCheckActivity.EXTRA_ALARM_ID, alarmId)
            }
            context.startActivity(checkIntent)
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
