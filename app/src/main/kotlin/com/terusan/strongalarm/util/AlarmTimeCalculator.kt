package com.terusan.strongalarm.util

import com.terusan.strongalarm.domain.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * アラームの次回鳴動時刻を計算するユーティリティ
 */
object AlarmTimeCalculator {

    /**
     * アラームが次回鳴動すべきエポックミリ秒を計算する
     */
    fun calculateNextTriggerTimeMillis(
        alarm: Alarm,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val nextTriggerDateTime = calculateNextTriggerDateTime(alarm, now)
        return nextTriggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * アラームが次回鳴動すべき LocalDateTime を計算する
     */
    fun calculateNextTriggerDateTime(
        alarm: Alarm,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val targetToday = now.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)

        // 曜日指定がない場合（単発アラーム）
        if (alarm.daysOfWeek.isEmpty()) {
            return if (targetToday.isAfter(now)) {
                targetToday
            } else {
                targetToday.plusDays(1)
            }
        }

        // 曜日指定がある場合
        // 今日を含む向こう7日間の中で最も近い曜日を探す
        for (dayOffset in 0..7) {
            val candidate = targetToday.plusDays(dayOffset.toLong())
            val candidateDayOfWeek = candidate.dayOfWeek

            if (alarm.daysOfWeek.contains(candidateDayOfWeek)) {
                // 当日の場合は現在時刻より未来である必要がある
                if (dayOffset == 0 && !candidate.isAfter(now)) {
                    continue
                }
                return candidate
            }
        }

        // フォールバック（通常到達しない）
        return targetToday.plusDays(1)
    }

    /**
     * スヌーズ次回鳴動時刻（エポックミリ秒）
     */
    fun calculateSnoozeTriggerTimeMillis(
        intervalMinutes: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val trigger = now.plusMinutes(intervalMinutes.toLong()).withSecond(0).withNano(0)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 起床確認トリガー時刻（エポックミリ秒）
     */
    fun calculateWakeUpCheckTriggerTimeMillis(
        minutesAfter: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): Long {
        val trigger = now.plusMinutes(minutesAfter.toLong()).withSecond(0).withNano(0)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
