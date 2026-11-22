package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ChronoRepository
import com.example.data.LapRecordEntity
import com.example.data.TimerPresetEntity
import com.example.util.HapticFeedbackController
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerPhase {
  Idle,
  Running,
  Paused,
  Finished,
}

data class TimerUiState(
  val phase: TimerPhase = TimerPhase.Idle,
  val totalDurationMillis: Long = 60_000L,
  val remainingMillis: Long = 60_000L,
  val lastNudgeSeconds: Int? = null,
) {
  val progressFraction: Float
    get() =
      if (totalDurationMillis <= 0L) 0f
      else (remainingMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
}

data class StopwatchUiState(
  val isRunning: Boolean = false,
  val elapsedMillis: Long = 0L,
  val lastLapTotalMillis: Long = 0L,
) {
  val sweepFraction: Float
    get() = ((elapsedMillis % 60_000L).toFloat() / 60_000f).coerceIn(0f, 1f)
}

class ChronoViewModel(
  private val repository: ChronoRepository,
  private val haptics: HapticFeedbackController,
  private val elapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() },
) : ViewModel() {

  val presets: StateFlow<List<TimerPresetEntity>> =
    repository.presets.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  val recentLaps: StateFlow<List<LapRecordEntity>> =
    repository.recentLaps.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  private val _timerState = MutableStateFlow(TimerUiState())
  val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

  private val _stopwatchState = MutableStateFlow(StopwatchUiState())
  val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()

  private var timerJob: Job? = null
  private var nudgeBadgeJob: Job? = null
  private var timerTargetRealtimeMs: Long = 0L
  private var rotaryAccumulatorPx: Float = 0f

  private var stopwatchJob: Job? = null
  private var stopwatchBaseRealtimeMs: Long = 0L
  private var stopwatchAccumulatedMs: Long = 0L

  init {
    viewModelScope.launch {
      repository.ensureSeededPresets()
    }
  }

  fun startTimerWithSeconds(durationSeconds: Int) {
    val safeSeconds = durationSeconds.coerceIn(5, 3599)
    val durationMillis = safeSeconds * 1000L
    haptics.click()
    viewModelScope.launch {
      repository.saveOrTouchPreset(safeSeconds)
    }
    rotaryAccumulatorPx = 0f
    timerTargetRealtimeMs = elapsedRealtimeMillis() + durationMillis
    _timerState.value =
      TimerUiState(
        phase = TimerPhase.Running,
        totalDurationMillis = durationMillis,
        remainingMillis = durationMillis,
        lastNudgeSeconds = null,
      )
    startTimerTicker()
  }

  fun toggleTimerPauseResume() {
    val current = _timerState.value
    when (current.phase) {
      TimerPhase.Running -> {
        timerJob?.cancel()
        val remaining = (timerTargetRealtimeMs - elapsedRealtimeMillis()).coerceAtLeast(0L)
        haptics.click()
        _timerState.update {
          it.copy(phase = TimerPhase.Paused, remainingMillis = remaining)
        }
      }
      TimerPhase.Paused -> {
        val remaining = current.remainingMillis.coerceAtLeast(1_000L)
        timerTargetRealtimeMs = elapsedRealtimeMillis() + remaining
        haptics.click()
        _timerState.update { it.copy(phase = TimerPhase.Running) }
        startTimerTicker()
      }
      TimerPhase.Finished -> restartFinishedTimer()
      TimerPhase.Idle -> Unit
    }
  }

  fun restartFinishedTimer() {
    val total = _timerState.value.totalDurationMillis.coerceAtLeast(5_000L)
    haptics.click()
    timerTargetRealtimeMs = elapsedRealtimeMillis() + total
    _timerState.value =
      TimerUiState(
        phase = TimerPhase.Running,
        totalDurationMillis = total,
        remainingMillis = total,
        lastNudgeSeconds = null,
      )
    startTimerTicker()
  }

  fun resetTimerToPresets() {
    timerJob?.cancel()
    nudgeBadgeJob?.cancel()
    rotaryAccumulatorPx = 0f
    haptics.click()
    _timerState.value = TimerUiState(phase = TimerPhase.Idle)
  }

  /** Adjusts active/paused timer in ±15s steps when user spins the watch bezel or crown. */
  fun nudgeTimerByRotary(verticalScrollPixels: Float) {
    val current = _timerState.value
    if (current.phase != TimerPhase.Running && current.phase != TimerPhase.Paused) return

    rotaryAccumulatorPx += verticalScrollPixels
    val detentThresholdPx = 44f
    if (abs(rotaryAccumulatorPx) < detentThresholdPx) return

    val steps = (rotaryAccumulatorPx / detentThresholdPx).toInt()
    rotaryAccumulatorPx -= steps * detentThresholdPx
    val deltaSeconds = steps * 15
    if (deltaSeconds == 0) return

    applyTimerNudgeSeconds(deltaSeconds)
  }

  fun applyTimerNudgeSeconds(deltaSeconds: Int) {
    val current = _timerState.value
    if (current.phase != TimerPhase.Running && current.phase != TimerPhase.Paused) return

    val deltaMs = deltaSeconds * 1000L
    val now = elapsedRealtimeMillis()
    val baseRemaining =
      if (current.phase == TimerPhase.Running) {
        (timerTargetRealtimeMs - now).coerceAtLeast(0L)
      } else {
        current.remainingMillis
      }

    val updatedRemaining = (baseRemaining + deltaMs).coerceIn(5_000L, 3_599_000L)
    if (current.phase == TimerPhase.Running) {
      timerTargetRealtimeMs = now + updatedRemaining
    }
    val updatedTotal = maxOf(current.totalDurationMillis, updatedRemaining)

    haptics.rotaryTick()
    _timerState.update {
      it.copy(
        totalDurationMillis = updatedTotal,
        remainingMillis = updatedRemaining,
        lastNudgeSeconds = deltaSeconds,
      )
    }

    nudgeBadgeJob?.cancel()
    nudgeBadgeJob =
      viewModelScope.launch {
        delay(900L)
        _timerState.update { it.copy(lastNudgeSeconds = null) }
      }
  }

  fun deleteCustomPreset(presetId: Int) {
    haptics.click()
    viewModelScope.launch {
      repository.deleteCustomPreset(presetId)
    }
  }

  private fun startTimerTicker() {
    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        while (true) {
          val remaining = (timerTargetRealtimeMs - elapsedRealtimeMillis()).coerceAtLeast(0L)
          if (remaining <= 0L) {
            _timerState.update {
              it.copy(
                phase = TimerPhase.Finished,
                remainingMillis = 0L,
                lastNudgeSeconds = null,
              )
            }
            haptics.timerFinishedAlarm()
            break
          } else {
            _timerState.update { it.copy(remainingMillis = remaining) }
          }
          delay(50L)
        }
      }
  }

  fun toggleStopwatch() {
    val current = _stopwatchState.value
    haptics.click()
    if (current.isRunning) {
      stopwatchJob?.cancel()
      val total = stopwatchAccumulatedMs + (elapsedRealtimeMillis() - stopwatchBaseRealtimeMs)
      stopwatchAccumulatedMs = total
      _stopwatchState.update { it.copy(isRunning = false, elapsedMillis = total) }
    } else {
      stopwatchBaseRealtimeMs = elapsedRealtimeMillis()
      _stopwatchState.update { it.copy(isRunning = true) }
      startStopwatchTicker()
    }
  }

  fun lapOrResetStopwatch() {
    val current = _stopwatchState.value
    if (current.isRunning) {
      val nowTotal = stopwatchAccumulatedMs + (elapsedRealtimeMillis() - stopwatchBaseRealtimeMs)
      val split = (nowTotal - current.lastLapTotalMillis).coerceAtLeast(0L)
      haptics.click()
      _stopwatchState.update {
        it.copy(elapsedMillis = nowTotal, lastLapTotalMillis = nowTotal)
      }
      viewModelScope.launch {
        repository.recordLap(splitMillis = split, totalMillis = nowTotal)
      }
    } else if (current.elapsedMillis > 0L || recentLaps.value.isNotEmpty()) {
      stopwatchJob?.cancel()
      stopwatchAccumulatedMs = 0L
      haptics.click()
      _stopwatchState.value = StopwatchUiState()
      viewModelScope.launch {
        repository.clearLaps()
      }
    }
  }

  fun clearLapsOnly() {
    haptics.click()
    _stopwatchState.update { it.copy(lastLapTotalMillis = 0L) }
    viewModelScope.launch {
      repository.clearLaps()
    }
  }

  private fun startStopwatchTicker() {
    stopwatchJob?.cancel()
    stopwatchJob =
      viewModelScope.launch {
        while (true) {
          val currentElapsed =
            stopwatchAccumulatedMs + (elapsedRealtimeMillis() - stopwatchBaseRealtimeMs)
          _stopwatchState.update { it.copy(elapsedMillis = currentElapsed) }
          delay(50L)
        }
      }
  }

  class Factory(
    private val repository: ChronoRepository,
    private val haptics: HapticFeedbackController,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ChronoViewModel(repository = repository, haptics = haptics) as T
    }
  }
}
