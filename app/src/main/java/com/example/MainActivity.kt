package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.data.ChronoDatabase
import com.example.data.ChronoRepository
import com.example.data.TimerPresetEntity
import com.example.ui.screens.CustomTimerPickerScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.WristChronoWatchTheme
import com.example.util.WatchHaptics
import com.example.viewmodel.ChronoViewModel
import com.example.viewmodel.TimerPhase
import com.example.viewmodel.TimerUiState

const val ROUTE_WATCH_DECK = "watch_deck"
const val ROUTE_CUSTOM_PICKER = "custom_picker"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val appContext = LocalContext.current.applicationContext
      val factory = remember(appContext) {
        val db = ChronoDatabase.getInstance(appContext)
        ChronoViewModel.Factory(
          repository = ChronoRepository(db.chronoDao()),
          haptics = WatchHaptics(appContext),
        )
      }
      val chronoViewModel: ChronoViewModel = viewModel(factory = factory)

      val timerState by chronoViewModel.timerState.collectAsStateWithLifecycle()
      val stopwatchState by chronoViewModel.stopwatchState.collectAsStateWithLifecycle()

      val keepScreenAwake =
        timerState.phase == TimerPhase.Running || stopwatchState.isRunning
      DisposableEffect(keepScreenAwake) {
        if (keepScreenAwake) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }

      WristChronoWatchTheme {
        WristChronoWatchApp(viewModel = chronoViewModel)
      }
    }
  }
}

@Composable
fun WristChronoWatchApp(viewModel: ChronoViewModel) {
  val navController = rememberSwipeDismissableNavController()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val stopwatchState by viewModel.stopwatchState.collectAsStateWithLifecycle()
  val presets by viewModel.presets.collectAsStateWithLifecycle()
  val recentLaps by viewModel.recentLaps.collectAsStateWithLifecycle()

  AppScaffold(
    timeText = { TimeText() },
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("watch_app_scaffold"),
  ) {
    SwipeDismissableNavHost(
      navController = navController,
      startDestination = ROUTE_WATCH_DECK,
    ) {
      composable(ROUTE_WATCH_DECK) {
        val pagerState = rememberPagerState(pageCount = { 2 })

        HorizontalPagerScaffold(
          pagerState = pagerState,
          modifier = Modifier.fillMaxSize().testTag("watch_horizontal_pager"),
        ) {
          HorizontalPager(
            state = pagerState,
            // Keep rotary bezel events routed to each page's vertical list or ±15s timer nudge
            rotaryScrollableBehavior = null,
          ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
              when (page) {
                0 ->
                  TimerScreen(
                    timerState = timerState,
                    presets = presets,
                    onSelectPreset = viewModel::startTimerWithSeconds,
                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                    onOpenCustomPicker = { navController.navigate(ROUTE_CUSTOM_PICKER) },
                    onTogglePauseResume = viewModel::toggleTimerPauseResume,
                    onResetTimer = viewModel::resetTimerToPresets,
                    onRestartFinished = viewModel::restartFinishedTimer,
                    onRotaryNudge = viewModel::nudgeTimerByRotary,
                  )
                else ->
                  StopwatchScreen(
                    stopwatchState = stopwatchState,
                    recentLaps = recentLaps,
                    onToggleStopwatch = viewModel::toggleStopwatch,
                    onLapOrReset = viewModel::lapOrResetStopwatch,
                    onClearLaps = viewModel::clearLapsOnly,
                  )
              }
            }
          }
        }
      }

      composable(ROUTE_CUSTOM_PICKER) {
        CustomTimerPickerScreen(
          onDurationPicked = { seconds ->
            navController.popBackStack()
            viewModel.startTimerWithSeconds(seconds)
          },
          onNavigateBack = { navController.popBackStack() },
        )
      }
    }
  }
}

@WearPreviewDevices
@Composable
private fun WristChronoActiveTimerPreview() {
  WristChronoWatchTheme {
    AppScaffold(timeText = { TimeText() }) {
      TimerScreen(
        timerState =
          TimerUiState(
            phase = TimerPhase.Running,
            totalDurationMillis = 180_000L,
            remainingMillis = 135_000L,
          ),
        presets =
          listOf(
            TimerPresetEntity(id = 1, durationSeconds = 60, isBuiltIn = true),
            TimerPresetEntity(id = 2, durationSeconds = 180, isBuiltIn = true),
          ),
        onSelectPreset = {},
        onDeleteCustomPreset = {},
        onOpenCustomPicker = {},
        onTogglePauseResume = {},
        onResetTimer = {},
        onRestartFinished = {},
        onRotaryNudge = {},
      )
    }
  }
}
