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

        // フォールバック
        return targetToday.plusDays(1)
    }

    /**
     * スヌーズ次回鳴動時刻（エポックミリ秒）
     * 秒を切り捨てず、現在時刻から指定分数後（正確なミリ秒）にセット
     */
    fun calculateSnoozeTriggerTimeMillis(
        intervalMinutes: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        return currentTimeMillis + intervalMinutes * 60 * 1000L
    }

    /**
     * 起床確認トリガー時刻（エポックミリ秒）
     * 秒を切り捨てず、現在時刻から指定分数後（正確なミリ秒）にセット
     */
    fun calculateWakeUpCheckTriggerTimeMillis(
        minutesAfter: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        return currentTimeMillis + minutesAfter * 60 * 1000L
    }
}
