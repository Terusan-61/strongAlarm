package com.terusan.strongalarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.terusan.strongalarm.domain.model.Alarm
import com.terusan.strongalarm.domain.model.MissionDifficulty
import com.terusan.strongalarm.domain.model.MissionType
import java.time.DayOfWeek

/**
 * アラーム設定のRoomデータベースエンティティ
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val daysOfWeek: Set<DayOfWeek>,
    val label: String,
    val volume: Int,
    val isGradualVolume: Boolean,
    val snoozeIntervalMinutes: Int,
    val maxSnoozeCount: Int,
    val currentSnoozeCount: Int,
    val missionType: MissionType,
    val missionDifficulty: MissionDifficulty,
    val checkWakeUp: Boolean,
    val checkWakeUpAfterMinutes: Int,
    val checkWakeUpSilent: Boolean = true,
    val isVibrate: Boolean
) {
    /**
     * エンティティからドメインモデルへ変換
     */
    fun toDomain(): Alarm = Alarm(
        id = id,
        hour = hour,
        minute = minute,
        isEnabled = isEnabled,
        daysOfWeek = daysOfWeek,
        label = label,
        volume = volume,
        isGradualVolume = isGradualVolume,
        snoozeIntervalMinutes = snoozeIntervalMinutes,
        maxSnoozeCount = maxSnoozeCount,
        currentSnoozeCount = currentSnoozeCount,
        missionType = missionType,
        missionDifficulty = missionDifficulty,
        checkWakeUp = checkWakeUp,
        checkWakeUpAfterMinutes = checkWakeUpAfterMinutes,
        checkWakeUpSilent = checkWakeUpSilent,
        isVibrate = isVibrate
    )

    companion object {
        /**
         * ドメインモデルからエンティティへ変換
         */
        fun fromDomain(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            isEnabled = alarm.isEnabled,
            daysOfWeek = alarm.daysOfWeek,
            label = alarm.label,
            volume = alarm.volume,
            isGradualVolume = alarm.isGradualVolume,
            snoozeIntervalMinutes = alarm.snoozeIntervalMinutes,
            maxSnoozeCount = alarm.maxSnoozeCount,
            currentSnoozeCount = alarm.currentSnoozeCount,
            missionType = alarm.missionType,
            missionDifficulty = alarm.missionDifficulty,
            checkWakeUp = alarm.checkWakeUp,
            checkWakeUpAfterMinutes = alarm.checkWakeUpAfterMinutes,
            checkWakeUpSilent = alarm.checkWakeUpSilent,
            isVibrate = alarm.isVibrate
        )
    }
}
