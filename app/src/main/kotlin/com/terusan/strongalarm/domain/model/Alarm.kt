package com.terusan.strongalarm.domain.model

import java.time.DayOfWeek

/**
 * 解除ミッションの種類
 */
enum class MissionType(val displayName: String, val description: String) {
    NONE("なし", "ボタンを押すだけで停止できます"),
    MATH("計算問題", "簡単な算数問題を解いて脳を目覚めさせます"),
    SHAKE("端末シェイク", "端末を一定回数激しく振って目覚めさせます"),
    BARCODE("バーコード / QR", "指定のバーコードを洗面所などでスキャンして目覚めさせます")
}

/**
 * ミッションの難易度
 */
enum class MissionDifficulty(val displayName: String) {
    EASY("やさしい"),
    MEDIUM("普通"),
    HARD("むずかしい")
}

/**
 * アラームのドメインモデル
 */
data class Alarm(
    val id: Long = 0,
    val hour: Int,                                    // 時 (0-23)
    val minute: Int,                                  // 分 (0-59)
    val isEnabled: Boolean = true,                    // 有効/無効フラグ
    val daysOfWeek: Set<DayOfWeek> = emptySet(),      // 繰り返し曜日（空なら1回のみ）
    val label: String = "アラーム",                   // アラーム名
    val volume: Int = 100,                            // 最大音量 (0-100)
    val isGradualVolume: Boolean = true,              // 徐々に音量を大きくするか
    val snoozeIntervalMinutes: Int = 5,               // スヌーズ間隔（分）
    val maxSnoozeCount: Int = 3,                      // スヌーズ上限回数（0はスヌーズ不可）
    val currentSnoozeCount: Int = 0,                  // 現在のスヌーズ回数
    val missionType: MissionType = MissionType.NONE,  // 解除ミッション種別
    val missionDifficulty: MissionDifficulty = MissionDifficulty.EASY, // 難易度
    val checkWakeUp: Boolean = true,                  // 起床確認を行うか
    val checkWakeUpAfterMinutes: Int = 10,            // 起床確認までの時間（分）
    val checkWakeUpSilent: Boolean = true,            // 起床確認を完全サイレント（無音・画面点灯のみ）で行うか
    val isVibrate: Boolean = true                     // バイブレーション有効化
) {
    /**
     * 表示用の時刻フォーマット (例: "07:05")
     */
    val formattedTime: String
        get() = "%02d:%02d".format(hour, minute)

    /**
     * 繰り返し曜日の表示用テキスト (例: "月, 火, 水" や "毎日", "平日")
     */
    val repeatSummary: String
        get() {
            if (daysOfWeek.isEmpty()) return "1回のみ"
            if (daysOfWeek.size == 7) return "毎日"
            val weekdays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
            val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            if (daysOfWeek == weekdays) return "平日"
            if (daysOfWeek == weekend) return "休日"

            val order = listOf(
                DayOfWeek.MONDAY to "月",
                DayOfWeek.TUESDAY to "火",
                DayOfWeek.WEDNESDAY to "水",
                DayOfWeek.THURSDAY to "木",
                DayOfWeek.FRIDAY to "金",
                DayOfWeek.SATURDAY to "土",
                DayOfWeek.SUNDAY to "日"
            )
            return order.filter { it.first in daysOfWeek }.joinToString(" ") { it.second }
        }
}
