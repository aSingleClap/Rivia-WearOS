package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.R
import com.example.data.LapRecordEntity
import com.example.util.TimeFormatters
import com.example.viewmodel.StopwatchUiState

@Composable
fun StopwatchScreen(
  stopwatchState: StopwatchUiState,
  recentLaps: List<LapRecordEntity>,
  onToggleStopwatch: () -> Unit,
  onLapOrReset: () -> Unit,
  onClearLaps: () -> Unit,
) {
  val listState = rememberTransformingLazyColumnState()
  val transformationSpec = rememberTransformationSpec()

  ScreenScaffold(scrollState = listState) { contentPadding ->
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        progress = { stopwatchState.sweepFraction },
        modifier =
          Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
        startAngle = 295f,
        endAngle = 245f,
        strokeWidth = 4.dp,
        colors =
          ProgressIndicatorDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
          ),
      )

      TransformingLazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize().testTag("stopwatch_list"),
      ) {
        item(key = "stopwatch_dial") {
          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            val topBadge =
              if (recentLaps.isNotEmpty()) {
                stringResource(R.string.label_lap_number, recentLaps.first().lapNumber + 1)
              } else {
                stringResource(R.string.header_stopwatch)
              }

            Text(
              text = topBadge,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = TimeFormatters.formatStopwatchTenths(stopwatchState.elapsedMillis),
              style = MaterialTheme.typography.displayMedium,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("stopwatch_elapsed_text"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            val canLapOrReset =
              stopwatchState.isRunning ||
                stopwatchState.elapsedMillis > 0L ||
                recentLaps.isNotEmpty()

            // Strictly two big circular action buttons
            Row(
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilledIconButton(
                onClick = onToggleStopwatch,
                shapes = IconButtonDefaults.animatedShapes(),
                modifier = Modifier.size(52.dp).testTag("stopwatch_toggle_button"),
              ) {
                Icon(
                  imageVector =
                    if (stopwatchState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription =
                    stringResource(
                      if (stopwatchState.isRunning) R.string.cd_stop_stopwatch
                      else R.string.cd_start_stopwatch
                    ),
                )
              }

              FilledTonalIconButton(
                onClick = onLapOrReset,
                enabled = canLapOrReset,
                modifier = Modifier.size(52.dp).testTag("stopwatch_lap_reset_button"),
              ) {
                Icon(
                  imageVector =
                    if (stopwatchState.isRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                  contentDescription =
                    stringResource(
                      if (stopwatchState.isRunning) R.string.cd_lap_stopwatch
                      else R.string.cd_reset_stopwatch
                    ),
                )
              }
            }
          }
        }

        items(items = recentLaps, key = { it.id }) { lap ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .transformedHeight(this, transformationSpec)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("lap_row_${lap.lapNumber}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.label_lap_number, lap.lapNumber),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
            Text(
              text = "+${TimeFormatters.formatStopwatchTenths(lap.splitMillis)}",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
            )
          }
        }

        if (recentLaps.isNotEmpty()) {
          item(key = "clear_laps") {
            FilledTonalButton(
              onClick = onClearLaps,
              transformation = SurfaceTransformation(transformationSpec),
              modifier =
                Modifier.fillMaxWidth()
                  .transformedHeight(this, transformationSpec)
                  .testTag("clear_laps_button"),
              label = {
                Text(
                  text = stringResource(R.string.action_clear_laps),
                  maxLines = 1,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth(),
                )
              },
            )
          }
        }
      }
    }
  }
}
