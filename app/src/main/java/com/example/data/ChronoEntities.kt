package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "timer_presets",
  indices = [Index(value = ["durationSeconds"], unique = true)],
)
data class TimerPresetEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val durationSeconds: Int,
  val isBuiltIn: Boolean = false,
  val lastUsedEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "stopwatch_laps")
data class LapRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val lapNumber: Int,
  val splitMillis: Long,
  val totalMillis: Long,
)
