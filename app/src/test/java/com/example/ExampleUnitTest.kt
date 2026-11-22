package com.example

import com.example.data.ChronoDao
import com.example.data.ChronoRepository
import com.example.data.LapRecordEntity
import com.example.data.TimerPresetEntity
import com.example.util.HapticFeedbackController
import com.example.util.TimeFormatters
import com.example.viewmodel.ChronoViewModel
import com.example.viewmodel.TimerPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  private val testDispatcher = StandardTestDispatcher()

  private class FakeChronoDao : ChronoDao {
    private val presetsFlow = MutableStateFlow<List<TimerPresetEntity>>(emptyList())
    private val lapsFlow = MutableStateFlow<List<LapRecordEntity>>(emptyList())
    private var nextId = 1

    override fun observePresets(): Flow<List<TimerPresetEntity>> = presetsFlow

    override suspend fun countPresets(): Int = presetsFlow.value.size

    override suspend fun findPresetByDuration(durationSeconds: Int): TimerPresetEntity? =
      presetsFlow.value.firstOrNull { it.durationSeconds == durationSeconds }

    override suspend fun upsertPreset(preset: TimerPresetEntity) {
      val current = presetsFlow.value.toMutableList()
      val existingIdx = current.indexOfFirst { it.durationSeconds == preset.durationSeconds }
      if (existingIdx >= 0) {
        current[existingIdx] = preset
      } else {
        current.add(preset.copy(id = nextId++))
      }
      presetsFlow.value = current
    }

    override suspend fun insertPresets(presets: List<TimerPresetEntity>) {
      presets.forEach { upsertPreset(it) }
    }

    override suspend fun deleteCustomPreset(id: Int) {
      presetsFlow.value = presetsFlow.value.filterNot { it.id == id && !it.isBuiltIn }
    }

    override fun observeRecentLaps(): Flow<List<LapRecordEntity>> = lapsFlow

    override suspend fun getMaxLapNumber(): Int = lapsFlow.value.maxOfOrNull { it.lapNumber } ?: 0

    override suspend fun insertLap(lap: LapRecordEntity) {
      val updated = (listOf(lap.copy(id = nextId++)) + lapsFlow.value).take(5)
      lapsFlow.value = updated
    }

    override suspend fun clearLaps() {
      lapsFlow.value = emptyList()
    }
  }

  private class RecordingHaptics : HapticFeedbackController {
    var clicks = 0
    var ticks = 0
    var alarms = 0

    override fun click() {
      clicks++
    }

    override fun rotaryTick() {
      ticks++
    }

    override fun timerFinishedAlarm() {
      alarms++
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `time formatters produce concise watch-friendly strings`() {
    assertEquals("03:00", TimeFormatters.formatPresetDuration(180))
    assertEquals("3m", TimeFormatters.formatPresetShortHint(180))
    assertEquals("00:45", TimeFormatters.formatCountdownMillis(44_200L))
    assertEquals("01:12.4", TimeFormatters.formatStopwatchTenths(72_450L))
  }

  @Test
  fun `timer countdown rotary nudge and alarm lifecycle work accurately`() =
    runTest(testDispatcher) {
      var fakeClockMs = 1_000L
      val haptics = RecordingHaptics()
      val repository = ChronoRepository(FakeChronoDao())
      val viewModel =
        ChronoViewModel(
          repository = repository,
          haptics = haptics,
          elapsedRealtimeMillis = { fakeClockMs },
        )
      runCurrent()

      viewModel.startTimerWithSeconds(60)
      assertEquals(TimerPhase.Running, viewModel.timerState.value.phase)
      assertEquals(60_000L, viewModel.timerState.value.remainingMillis)

      // Spin watch bezel clockwise (+15s)
      viewModel.nudgeTimerByRotary(50f)
      assertEquals(75_000L, viewModel.timerState.value.remainingMillis)
      assertTrue(haptics.ticks >= 1)

      // Advance monotonic clock past target to trigger Finished state + wrist alarm
      fakeClockMs += 75_500L
      advanceTimeBy(100L)
      runCurrent()

      assertEquals(TimerPhase.Finished, viewModel.timerState.value.phase)
      assertEquals(0L, viewModel.timerState.value.remainingMillis)
      assertEquals(1, haptics.alarms)

      viewModel.resetTimerToPresets()
      assertEquals(TimerPhase.Idle, viewModel.timerState.value.phase)
    }

  @Test
  fun `stopwatch records lap splits and resets cleanly`() =
    runTest(testDispatcher) {
      var fakeClockMs = 5_000L
      val dao = FakeChronoDao()
      val repository = ChronoRepository(dao)
      val viewModel =
        ChronoViewModel(
          repository = repository,
          haptics = RecordingHaptics(),
          elapsedRealtimeMillis = { fakeClockMs },
        )
      runCurrent()

      viewModel.toggleStopwatch()
      assertTrue(viewModel.stopwatchState.value.isRunning)

      fakeClockMs += 12_400L
      viewModel.lapOrResetStopwatch()
      runCurrent()
      assertEquals(12_400L, viewModel.stopwatchState.value.lastLapTotalMillis)

      // Pause and reset
      viewModel.toggleStopwatch()
      viewModel.lapOrResetStopwatch()
      runCurrent()
      assertEquals(0L, viewModel.stopwatchState.value.elapsedMillis)
      assertEquals(0, dao.getMaxLapNumber())
    }
}
