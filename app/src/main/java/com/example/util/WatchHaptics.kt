package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

interface HapticFeedbackController {
  fun click()
  fun rotaryTick()
  fun timerFinishedAlarm()
}

class WatchHaptics(context: Context) : HapticFeedbackController {

  private val vibrator: Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  override fun click() {
    vibrateSafe {
      VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    }
  }

  override fun rotaryTick() {
    vibrateSafe {
      VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    }
  }

  override fun timerFinishedAlarm() {
    vibrateSafe {
      VibrationEffect.createWaveform(
        longArrayOf(0L, 180L, 100L, 180L, 100L, 260L),
        intArrayOf(0, 255, 0, 255, 0, 255),
        -1,
      )
    }
  }

  private inline fun vibrateSafe(effectBuilder: () -> VibrationEffect) {
    runCatching {
      val vib = vibrator ?: return
      if (vib.hasVibrator()) {
        vib.vibrate(effectBuilder())
      }
    }
  }
}
