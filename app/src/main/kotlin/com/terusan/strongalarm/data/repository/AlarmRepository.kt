package com.terusan.strongalarm.data.repository

import com.terusan.strongalarm.data.local.AlarmDao
import com.terusan.strongalarm.data.local.AlarmEntity
import com.terusan.strongalarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * アラームデータ操作のリポジトリインターフェース
 */
interface AlarmRepository {
    val alarmsFlow: Flow<List<Alarm>>
    suspend fun getAlarmById(id: Long): Alarm?
    suspend fun getEnabledAlarms(): List<Alarm>
    suspend fun saveAlarm(alarm: Alarm): Long
    suspend fun toggleAlarm(id: Long, isEnabled: Boolean)
    suspend fun updateSnoozeCount(id: Long, count: Int)
    suspend fun deleteAlarm(alarm: Alarm)
}

/**
 * Room DAO を使用したリポジトリ実装
 */
class AlarmRepositoryImpl(
    private val dao: AlarmDao
) : AlarmRepository {

    override val alarmsFlow: Flow<List<Alarm>> = dao.getAllAlarmsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getAlarmById(id: Long): Alarm? {
        return dao.getAlarmById(id)?.toDomain()
    }

    override suspend fun getEnabledAlarms(): List<Alarm> {
        return dao.getEnabledAlarms().map { it.toDomain() }
    }

    override suspend fun saveAlarm(alarm: Alarm): Long {
        return dao.insertAlarm(AlarmEntity.fromDomain(alarm))
    }

    override suspend fun toggleAlarm(id: Long, isEnabled: Boolean) {
        dao.updateEnabled(id, isEnabled)
    }

    override suspend fun updateSnoozeCount(id: Long, count: Int) {
        dao.updateSnoozeCount(id, count)
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        dao.deleteAlarm(AlarmEntity.fromDomain(alarm))
    }
}
