package com.example.data

import kotlinx.coroutines.flow.Flow

class ChronoRepository(private val dao: ChronoDao) {

  val presets: Flow<List<TimerPresetEntity>> = dao.observePresets()

  val recentLaps: Flow<List<LapRecordEntity>> = dao.observeRecentLaps()

  suspend fun ensureSeededPresets() {
    if (dao.countPresets() == 0) {
      val now = System.currentTimeMillis()
      dao.insertPresets(
        listOf(
          TimerPresetEntity(durationSeconds = 60, isBuiltIn = true, lastUsedEpochMs = now),
          TimerPresetEntity(durationSeconds = 180, isBuiltIn = true, lastUsedEpochMs = now - 1),
          TimerPresetEntity(durationSeconds = 300, isBuiltIn = true, lastUsedEpochMs = now - 2),
          TimerPresetEntity(durationSeconds = 600, isBuiltIn = true, lastUsedEpochMs = now - 3),
        )
      )
    }
  }

  suspend fun saveOrTouchPreset(durationSeconds: Int) {
    val clamped = durationSeconds.coerceAtLeast(5)
    val existing = dao.findPresetByDuration(clamped)
    if (existing != null) {
      dao.upsertPreset(existing.copy(lastUsedEpochMs = System.currentTimeMillis()))
    } else {
      dao.upsertPreset(
        TimerPresetEntity(
          durationSeconds = clamped,
          isBuiltIn = false,
          lastUsedEpochMs = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun deleteCustomPreset(id: Int) {
    dao.deleteCustomPreset(id)
  }

  suspend fun recordLap(splitMillis: Long, totalMillis: Long) {
    val nextLap = dao.getMaxLapNumber() + 1
    dao.insertLap(
      LapRecordEntity(
        lapNumber = nextLap,
        splitMillis = splitMillis,
        totalMillis = totalMillis,
      )
    )
  }

  suspend fun clearLaps() {
    dao.clearLaps()
  }
}
