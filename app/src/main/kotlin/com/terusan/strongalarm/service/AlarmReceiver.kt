package com.terusan.strongalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.terusan.strongalarm.MainActivity

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

        // 1. 鳴動用フォアグラウンドサービスを起動
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_START_RINGING
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingService.EXTRA_IS_WAKEUP_CHECK, isWakeUpCheck)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2. 画面点灯 & フルスクリーンActivity起動
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmRingingService.EXTRA_IS_WAKEUP_CHECK, isWakeUpCheck)
        }
        context.startActivity(activityIntent)
    }
}
