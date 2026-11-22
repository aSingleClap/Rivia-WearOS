package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PickerGroup
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.example.R
import java.util.Locale

@Composable
fun CustomTimerPickerScreen(
  onDurationPicked: (Int) -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  var selectedColumn by remember { mutableIntStateOf(0) }
  val minutesPickerState =
    rememberPickerState(initialNumberOfOptions = 60, initiallySelectedIndex = 2)
  // 12 options = 0s..55s in fast 5-second rotary bezel increments
  val fiveSecStepsPickerState =
    rememberPickerState(initialNumberOfOptions = 12, initiallySelectedIndex = 0)

  val minLabel = stringResource(R.string.label_minutes)
  val secLabel = stringResource(R.string.label_seconds)

  ScreenScaffold { contentPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(contentPadding)
          .testTag("custom_time_picker"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = if (selectedColumn == 0) minLabel else secLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )

      Spacer(modifier = Modifier.height(4.dp))

      PickerGroup(
        selectedPickerState =
          if (selectedColumn == 0) minutesPickerState else fiveSecStepsPickerState,
        autoCenter = false,
      ) {
        PickerGroupItem(
          pickerState = minutesPickerState,
          selected = selectedColumn == 0,
          onSelected = { selectedColumn = 0 },
          contentDescription = { minLabel },
          modifier = Modifier.size(68.dp, 88.dp),
          option = { optionIndex, isSelected ->
            Text(
              text = String.format(Locale.US, "%02d", optionIndex),
              style = MaterialTheme.typography.displaySmall,
              color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
        )

        PickerGroupItem(
          pickerState = fiveSecStepsPickerState,
          selected = selectedColumn == 1,
          onSelected = { selectedColumn = 1 },
          contentDescription = { secLabel },
          modifier = Modifier.size(68.dp, 88.dp),
          option = { optionIndex, isSelected ->
            Text(
              text = String.format(Locale.US, "%02d", optionIndex * 5),
              style = MaterialTheme.typography.displaySmall,
              color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      FilledIconButton(
        onClick = {
          val minutes = minutesPickerState.selectedOptionIndex
          val seconds = fiveSecStepsPickerState.selectedOptionIndex * 5
          val totalSeconds = (minutes * 60 + seconds).coerceAtLeast(5)
          onDurationPicked(totalSeconds)
        },
        shapes = IconButtonDefaults.animatedShapes(),
        modifier = Modifier.size(48.dp).testTag("confirm_custom_timer_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = stringResource(R.string.cd_confirm_duration),
        )
      }
    }
  }
}
