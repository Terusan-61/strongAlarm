package com.terusan.strongalarm.util

import com.terusan.strongalarm.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * アラーム次回鳴動時刻計算のユニットテスト
 */
class AlarmTimeCalculatorTest {

    @Test
    fun calculateNextTriggerDateTime_singleAlarm_sameDay_future() {
        val now = LocalDateTime.of(2026, 11, 4, 6, 0)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            daysOfWeek = emptySet() // 1回のみ
        )

        val next = AlarmTimeCalculator.calculateNextTriggerDateTime(alarm, now)
        assertEquals(LocalDateTime.of(2026, 11, 4, 7, 0), next)
    }

    @Test
    fun calculateNextTriggerDateTime_singleAlarm_pastTime_schedulesNextDay() {
        val now = LocalDateTime.of(2026, 11, 4, 8, 0)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            daysOfWeek = emptySet()
        )

        val next = AlarmTimeCalculator.calculateNextTriggerDateTime(alarm, now)
        assertEquals(LocalDateTime.of(2026, 11, 5, 7, 0), next)
    }

    @Test
    fun calculateNextTriggerDateTime_repeatingWeekdays() {
        // 2026/11/4 は水曜日 (Wednesday)
        val wednesdayNow = LocalDateTime.of(2026, 11, 4, 6, 0)
        val alarm = Alarm(
            hour = 7,
            minute = 0,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )

        // 今日の7:00が候補
        val next = AlarmTimeCalculator.calculateNextTriggerDateTime(alarm, wednesdayNow)
        assertEquals(LocalDateTime.of(2026, 11, 4, 7, 0), next)
    }
}
