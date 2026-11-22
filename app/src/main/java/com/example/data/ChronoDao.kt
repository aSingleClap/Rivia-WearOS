package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChronoDao {
  @Query("SELECT * FROM timer_presets ORDER BY isBuiltIn ASC, durationSeconds ASC LIMIT 6")
  fun observePresets(): Flow<List<TimerPresetEntity>>

  @Query("SELECT COUNT(*) FROM timer_presets")
  suspend fun countPresets(): Int

  @Query("SELECT * FROM timer_presets WHERE durationSeconds = :durationSeconds LIMIT 1")
  suspend fun findPresetByDuration(durationSeconds: Int): TimerPresetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPreset(preset: TimerPresetEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertPresets(presets: List<TimerPresetEntity>)

  @Query("DELETE FROM timer_presets WHERE id = :id AND isBuiltIn = 0")
  suspend fun deleteCustomPreset(id: Int)

  @Query("SELECT * FROM stopwatch_laps ORDER BY lapNumber DESC LIMIT 5")
  fun observeRecentLaps(): Flow<List<LapRecordEntity>>

  @Query("SELECT COALESCE(MAX(lapNumber), 0) FROM stopwatch_laps")
  suspend fun getMaxLapNumber(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLap(lap: LapRecordEntity)

  @Query("DELETE FROM stopwatch_laps")
  suspend fun clearLaps()
}
