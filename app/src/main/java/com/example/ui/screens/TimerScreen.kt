package com.example.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.R
import com.example.data.TimerPresetEntity
import com.example.util.TimeFormatters
import com.example.viewmodel.TimerPhase
import com.example.viewmodel.TimerUiState

@Composable
fun TimerScreen(
  timerState: TimerUiState,
  presets: List<TimerPresetEntity>,
  onSelectPreset: (Int) -> Unit,
  onDeleteCustomPreset: (Int) -> Unit,
  onOpenCustomPicker: () -> Unit,
  onTogglePauseResume: () -> Unit,
  onResetTimer: () -> Unit,
  onRestartFinished: () -> Unit,
  onRotaryNudge: (Float) -> Unit,
) {
  if (timerState.phase == TimerPhase.Idle) {
    TimerPresetListContent(
      presets = presets,
      onSelectPreset = onSelectPreset,
      onDeleteCustomPreset = onDeleteCustomPreset,
      onOpenCustomPicker = onOpenCustomPicker,
    )
  } else {
    ActiveTimerRingContent(
      timerState = timerState,
      onTogglePauseResume = onTogglePauseResume,
      onResetTimer = onResetTimer,
      onRestartFinished = onRestartFinished,
      onRotaryNudge = onRotaryNudge,
    )
  }
}

@Composable
private fun TimerPresetListContent(
  presets: List<TimerPresetEntity>,
  onSelectPreset: (Int) -> Unit,
  onDeleteCustomPreset: (Int) -> Unit,
  onOpenCustomPicker: () -> Unit,
) {
  val listState = rememberTransformingLazyColumnState()
  val transformationSpec = rememberTransformationSpec()

  ScreenScaffold(
    scrollState = listState,
    edgeButtonSpacing = 12.dp,
    edgeButton = {
      EdgeButton(
        onClick = onOpenCustomPicker,
        buttonSize = EdgeButtonSize.Small,
        modifier =
          Modifier.testTag("custom_timer_button")
            .scrollable(
              state = listState,
              orientation = Orientation.Vertical,
              reverseDirection = true,
              overscrollEffect = rememberOverscrollEffect(),
            ),
      ) {
        Text(
          text = stringResource(R.string.action_custom_timer),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
  ) { contentPadding ->
    TransformingLazyColumn(
      state = listState,
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxSize().testTag("timer_preset_list"),
    ) {
      item(key = "timer_header") {
        ListHeader(
          modifier =
            Modifier.fillMaxWidth()
              .transformedHeight(this, transformationSpec),
          transformation = SurfaceTransformation(transformationSpec),
        ) {
          Text(
            text = stringResource(R.string.header_timer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      items(items = presets, key = { it.id }) { preset ->
        val formattedDuration = TimeFormatters.formatPresetDuration(preset.durationSeconds)
        val shortHint = TimeFormatters.formatPresetShortHint(preset.durationSeconds)

        if (preset.isBuiltIn) {
          FilledTonalButton(
            onClick = { onSelectPreset(preset.durationSeconds) },
            transformation = SurfaceTransformation(transformationSpec),
            modifier =
              Modifier.fillMaxWidth()
                .transformedHeight(this, transformationSpec)
                .testTag("preset_button_${preset.durationSeconds}"),
            label = {
              Text(
                text = formattedDuration,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
              )
            },
            secondaryLabel = {
              Text(
                text = shortHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            },
          )
        } else {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .transformedHeight(this, transformationSpec),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Button(
              onClick = { onSelectPreset(preset.durationSeconds) },
              modifier =
                Modifier.weight(1f).testTag("preset_button_${preset.durationSeconds}"),
              label = {
                Text(
                  text = formattedDuration,
                  style = MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                )
              },
            )
            FilledTonalIconButton(
              onClick = { onDeleteCustomPreset(preset.id) },
              modifier =
                Modifier.size(48.dp).testTag("delete_preset_${preset.durationSeconds}"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_delete_preset, formattedDuration),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveTimerRingContent(
  timerState: TimerUiState,
  onTogglePauseResume: () -> Unit,
  onResetTimer: () -> Unit,
  onRestartFinished: () -> Unit,
  onRotaryNudge: (Float) -> Unit,
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(timerState.phase) {
    runCatching { focusRequester.requestFocus() }
  }

  ScreenScaffold { contentPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .onRotaryScrollEvent { event ->
            onRotaryNudge(event.verticalScrollPixels)
            true
          }
          .focusRequester(focusRequester)
          .focusable()
          .testTag("active_timer_container"),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        progress = {
          if (timerState.phase == TimerPhase.Finished) 1f else timerState.progressFraction
        },
        modifier =
          Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
        startAngle = 295f,
        endAngle = 245f,
        strokeWidth = 6.dp,
        colors =
          ProgressIndicatorDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
          ),
      )

      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        val statusLabel =
          when {
            timerState.lastNudgeSeconds != null -> {
              val sec = timerState.lastNudgeSeconds
              if (sec > 0) stringResource(R.string.label_nudge_plus, sec)
              else stringResource(R.string.label_nudge_minus, sec)
            }
            timerState.phase == TimerPhase.Running -> stringResource(R.string.status_running)
            timerState.phase == TimerPhase.Paused -> stringResource(R.string.status_paused)
            else -> stringResource(R.string.status_done)
          }

        Text(
          text = statusLabel,
          style = MaterialTheme.typography.labelMedium,
          color =
            if (timerState.phase == TimerPhase.Finished || timerState.lastNudgeSeconds != null) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          maxLines = 1,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = TimeFormatters.formatCountdownMillis(timerState.remainingMillis),
          style = MaterialTheme.typography.displayLarge,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("timer_countdown_text"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Strictly two big circular action buttons for round watch ergonomics
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (timerState.phase == TimerPhase.Finished) {
            FilledIconButton(
              onClick = onRestartFinished,
              shapes = IconButtonDefaults.animatedShapes(),
              modifier = Modifier.size(52.dp).testTag("timer_primary_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.cd_restart_timer),
              )
            }
            FilledTonalIconButton(
              onClick = onResetTimer,
              modifier = Modifier.size(52.dp).testTag("timer_reset_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_dismiss_timer),
              )
            }
          } else {
            val isRunning = timerState.phase == TimerPhase.Running
            FilledIconButton(
              onClick = onTogglePauseResume,
              shapes = IconButtonDefaults.animatedShapes(),
              modifier = Modifier.size(52.dp).testTag("timer_primary_button"),
            ) {
              Icon(
                imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription =
                  stringResource(
                    if (isRunning) R.string.cd_pause_timer else R.string.cd_resume_timer
                  ),
              )
            }
            FilledTonalIconButton(
              onClick = onResetTimer,
              modifier = Modifier.size(52.dp).testTag("timer_reset_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_reset_timer),
              )
            }
          }
        }
      }
    }
  }
}
