package com.example.util

import java.util.Locale

object TimeFormatters {

  /** Concise pill label for preset list, e.g., "01:00" or "02:30". */
  fun formatPresetDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  /** Short secondary hint for preset pills, e.g., "1 min" or "2m 30s". */
  fun formatPresetShortHint(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return when {
      minutes > 0 && seconds == 0 -> "${minutes}m"
      minutes == 0 -> "${seconds}s"
      else -> "${minutes}m ${seconds}s"
    }
  }

  /** Large center watch countdown readout (ceiled to whole seconds while running). */
  fun formatCountdownMillis(remainingMillis: Long): String {
    val totalSeconds = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  /** Compact tenth-of-a-second stopwatch display tailored for round watch screens. */
  fun formatStopwatchTenths(millis: Long): String {
    val safeMillis = millis.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1000L
    val tenths = (safeMillis % 1000L) / 100L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
  }
}
