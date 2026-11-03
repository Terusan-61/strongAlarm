package com.terusan.strongalarm.data.local

import androidx.room.TypeConverter
import com.terusan.strongalarm.domain.model.MissionDifficulty
import com.terusan.strongalarm.domain.model.MissionType
import java.time.DayOfWeek

/**
 * Room用の型コンバーター
 */
class Converters {
    /**
     * 曜日Setをカンマ区切りの文字列に変換
     */
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    /**
     * カンマ区切りの文字列を曜日Setに復元
     */
    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> {
        if (value.isBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { name ->
                runCatching { DayOfWeek.valueOf(name) }.getOrNull()
            }
            .toSet()
    }

    @TypeConverter
    fun fromMissionType(type: MissionType): String = type.name

    @TypeConverter
    fun toMissionType(value: String): MissionType =
        runCatching { MissionType.valueOf(value) }.getOrDefault(MissionType.NONE)

    @TypeConverter
    fun fromMissionDifficulty(difficulty: MissionDifficulty): String = difficulty.name

    @TypeConverter
    fun toMissionDifficulty(value: String): MissionDifficulty =
        runCatching { MissionDifficulty.valueOf(value) }.getOrDefault(MissionDifficulty.EASY)
}
