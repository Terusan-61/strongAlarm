package com.terusan.strongalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.terusan.strongalarm.MainActivity
import com.terusan.strongalarm.R
import com.terusan.strongalarm.data.local.StrongAlarmDatabase
import com.terusan.strongalarm.data.repository.AlarmRepositoryImpl
import com.terusan.strongalarm.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * アラーム鳴動・音量制御・バイブレーション・放置時再鳴動を統括するフォアグラウンドサービス
 */
class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentAlarm: Alarm? = null
    private var isWakeUpCheck: Boolean = false

    // 徐々に音量を上げる処理用
    private var currentGradualVolume = 0.1f
    private val volumeStepRunnable = object : Runnable {
        override fun run() {
            if (_isMuted.value) return
            val targetVolume = (currentAlarm?.volume ?: 100) / 100f
            if (currentGradualVolume < targetVolume) {
                currentGradualVolume = (currentGradualVolume + 0.05f).coerceAtMost(targetVolume)
                mediaPlayer?.setVolume(currentGradualVolume, currentGradualVolume)
                handler.postDelayed(this, 1500) // 1.5秒ごとにステップアップ
            }
        }
    }

    // アクション中の放置検知タイマー（無操作で25秒経過したら音を再開＆リセット）
    private val idleTimeoutRunnable = Runnable {
        Log.w(TAG, "アクション中の操作が途絶えたため、アラームを再鳴動しリセットします！")
        resumeRingingAfterIdleTimeout()
    }

    companion object {
        const val TAG = "AlarmRingingService"
        const val CHANNEL_ID = "strong_alarm_ringing_channel"
        const val NOTIFICATION_ID = 9999

        const val ACTION_START_RINGING = "action_start_ringing"
        const val ACTION_PAUSE_FOR_MISSION = "action_pause_for_mission" // アクション中消音
        const val ACTION_USER_INTERACTION = "action_user_interaction"   // アクション中の操作検知
        const val ACTION_DISMISS = "action_dismiss"                     // 解除
        const val ACTION_SNOOZE = "action_snooze"                       // スヌーズ

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_WAKEUP_CHECK = "extra_is_wakeup_check"

        // サービスの状態をUIと共有するためのStateFlow
        private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
        val ringingAlarm: StateFlow<Alarm?> = _ringingAlarm.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _missionResetTrigger = MutableStateFlow(0)
        val missionResetTrigger: StateFlow<Int> = _missionResetTrigger.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initVibrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        isWakeUpCheck = intent?.getBooleanExtra(EXTRA_IS_WAKEUP_CHECK, false) ?: false

        when (action) {
            ACTION_START_RINGING -> {
                startForeground(NOTIFICATION_ID, createNotification("アラーム鳴動中"))
                loadAndStartAlarm(alarmId)
            }
            ACTION_PAUSE_FOR_MISSION -> {
                pauseSoundForMission()
            }
            ACTION_USER_INTERACTION -> {
                resetIdleTimeout()
            }
            ACTION_DISMISS -> {
                dismissAlarm()
            }
            ACTION_SNOOZE -> {
                snoozeAlarm()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * アラームデータを読み込み、鳴動を開始する
     */
    private fun loadAndStartAlarm(alarmId: Long) {
        serviceScope.launch {
            val db = StrongAlarmDatabase.getDatabase(applicationContext)
            val repository = AlarmRepositoryImpl(db.alarmDao())
            val alarm = repository.getAlarmById(alarmId)

            if (alarm == null) {
                Log.e(TAG, "アラーム(ID: $alarmId) が見つかりませんでした")
                stopSelf()
                return@launch
            }

            currentAlarm = alarm
            _ringingAlarm.value = alarm
            _isMuted.value = false

            startAudioAndVibration(alarm)
        }
    }

    /**
     * 音声とバイブレーションの開始
     */
    private fun startAudioAndVibration(alarm: Alarm) {
        try {
            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
            }

            if (alarm.isGradualVolume) {
                currentGradualVolume = 0.1f
                mediaPlayer?.setVolume(currentGradualVolume, currentGradualVolume)
                mediaPlayer?.start()
                handler.removeCallbacks(volumeStepRunnable)
                handler.postDelayed(volumeStepRunnable, 1500)
            } else {
                val vol = alarm.volume / 100f
                mediaPlayer?.setVolume(vol, vol)
                mediaPlayer?.start()
            }

            if (alarm.isVibrate) {
                startVibration()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer の再生に失敗しました", e)
        }
    }

    /**
     * アクション中の消音処理（近所迷惑防止）
     */
    private fun pauseSoundForMission() {
        Log.d(TAG, "アクション中につき音声を一時停止します")
        _isMuted.value = true
        mediaPlayer?.setVolume(0f, 0f)
        stopVibration()

        // 25秒間放置されたら再鳴動・リセットするタイマーをセット
        handler.removeCallbacks(idleTimeoutRunnable)
        handler.postDelayed(idleTimeoutRunnable, 25_000)
    }

    /**
     * アクション中の操作があった場合に放置タイマーをリセット
     */
    private fun resetIdleTimeout() {
        if (_isMuted.value) {
            handler.removeCallbacks(idleTimeoutRunnable)
            handler.postDelayed(idleTimeoutRunnable, 25_000)
        }
    }

    /**
     * 放置タイムアウトによる再鳴動＆リセット
     */
    private fun resumeRingingAfterIdleTimeout() {
        _isMuted.value = false
        _missionResetTrigger.value += 1 // UI側にミッションリセット通知

        // 最大音量で再開
        val targetVolume = (currentAlarm?.volume ?: 100) / 100f
        mediaPlayer?.setVolume(targetVolume, targetVolume)
        if (currentAlarm?.isVibrate == true) {
            startVibration()
        }
    }

    /**
     * アラーム停止（ミッション完了）
     */
    private fun dismissAlarm() {
        handler.removeCallbacksAndMessages(null)
        stopAudioAndVibration()

        val alarm = currentAlarm
        if (alarm != null) {
            val scheduler = AlarmScheduler(applicationContext)

            // 単発アラームなら無効化、繰り返しなら次回を予約
            serviceScope.launch {
                val db = StrongAlarmDatabase.getDatabase(applicationContext)
                val repository = AlarmRepositoryImpl(db.alarmDao())

                if (alarm.daysOfWeek.isEmpty()) {
                    repository.toggleAlarm(alarm.id, false)
                } else {
                    repository.updateSnoozeCount(alarm.id, 0)
                    scheduler.schedule(alarm)
                }

                // 起床確認が有効な場合は、指定時間後に起床確認をスケジュール！
                if (alarm.checkWakeUp && !isWakeUpCheck) {
                    scheduler.scheduleWakeUpCheck(alarm)
                    Log.d(TAG, "起床確認アラームを ${alarm.checkWakeUpAfterMinutes}分後にスケジュールしました")
                }
            }
        }

        _ringingAlarm.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * スヌーズ実行
     */
    private fun snoozeAlarm() {
        handler.removeCallbacksAndMessages(null)
        stopAudioAndVibration()

        val alarm = currentAlarm
        if (alarm != null) {
            val scheduler = AlarmScheduler(applicationContext)
            serviceScope.launch {
                val db = StrongAlarmDatabase.getDatabase(applicationContext)
                val repository = AlarmRepositoryImpl(db.alarmDao())

                val nextSnoozeCount = alarm.currentSnoozeCount + 1
                repository.updateSnoozeCount(alarm.id, nextSnoozeCount)
                scheduler.scheduleSnooze(alarm)
            }
        }

        _ringingAlarm.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 300, 500, 300, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun stopAudioAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            stopVibration()
        } catch (e: Exception) {
            Log.e(TAG, "停止処理エラー", e)
        }
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "アラーム鳴動通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "アラーム鳴動中に表示される高優先度通知です"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentTitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText("タップしてアラーム画面を開きます")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        stopAudioAndVibration()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
