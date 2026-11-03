package com.terusan.strongalarm.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * アラームテーブルへのアクセスインターフェース
 */
@Dao
interface AlarmDao {
    /**
     * 全てのアラームを時刻順に監視取得
     */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>

    /**
     * 有効なアラーム一覧を取得（端末再起動時の復元用）
     */
    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    /**
     * IDでアラームを単一取得
     */
    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    /**
     * 新規アラームの挿入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    /**
     * アラームの更新
     */
    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    /**
     * アラームの有効/無効トグル
     */
    @Query("UPDATE alarms SET isEnabled = :isEnabled, currentSnoozeCount = 0 WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnabled: Boolean)

    /**
     * 現在のスヌーズ回数を更新
     */
    @Query("UPDATE alarms SET currentSnoozeCount = :snoozeCount WHERE id = :id")
    suspend fun updateSnoozeCount(id: Long, snoozeCount: Int)

    /**
     * アラームの削除
     */
    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)
}
