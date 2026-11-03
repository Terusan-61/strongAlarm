package com.terusan.strongalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.terusan.strongalarm.data.local.StrongAlarmDatabase
import com.terusan.strongalarm.data.repository.AlarmRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 端末再起動時に有効なアラームを自動復元するレシーバー
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "端末再起動を検知しました。有効なアラームを再スケジュールします")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = StrongAlarmDatabase.getDatabase(context)
                    val repository = AlarmRepositoryImpl(db.alarmDao())
                    val scheduler = AlarmScheduler(context)

                    val enabledAlarms = repository.getEnabledAlarms()
                    for (alarm in enabledAlarms) {
                        scheduler.schedule(alarm)
                        Log.d(TAG, "再復元: アラーム(ID: ${alarm.id}) を ${alarm.formattedTime} に再登録")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "アラームの再スケジュール中にエラーが発生しました", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
