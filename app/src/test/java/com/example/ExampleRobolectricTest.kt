package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ChronoDatabase
import com.example.data.ChronoRepository
import com.example.ui.theme.WristChronoWatchTheme
import com.example.util.HapticFeedbackController
import com.example.viewmodel.ChronoViewModel
import com.example.viewmodel.TimerPhase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var database: ChronoDatabase
  private lateinit var repository: ChronoRepository

  private val noOpHaptics =
    object : HapticFeedbackController {
      override fun click() = Unit
      override fun rotaryTick() = Unit
      override fun timerFinishedAlarm() = Unit
    }

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, ChronoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = ChronoRepository(database.chronoDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `watch app name and manifest hardware feature are configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("WristChrono", context.getString(R.string.app_name))

    val packageInfo =
      context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA,
      )
    val declaresWatchFeature =
      packageInfo.reqFeatures?.any { it.name == "android.hardware.type.watch" } == true
    assertTrue("Expected android.hardware.type.watch in AndroidManifest.xml", declaresWatchFeature)

    val appInfo =
      context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
      )
    assertTrue(
      "Expected standalone watch meta-data",
      appInfo.metaData?.getBoolean("com.google.android.wearable.standalone") == true,
    )
  }

  @Test
  fun `selecting preset starts active countdown and rotary nudge adjusts duration`() {
    var fakeNow = 10_000L
    val viewModel =
      ChronoViewModel(
        repository = repository,
        haptics = noOpHaptics,
        elapsedRealtimeMillis = { fakeNow },
      )

    composeTestRule.setContent {
      WristChronoWatchTheme {
        WristChronoWatchApp(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("preset_button_60").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals(TimerPhase.Running, viewModel.timerState.value.phase)
    composeTestRule.onNodeWithTag("active_timer_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timer_primary_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timer_reset_button").assertIsDisplayed()

    // Simulate bezel rotary scroll (+15s)
    viewModel.nudgeTimerByRotary(50f)
    composeTestRule.waitForIdle()
    assertEquals(75_000L, viewModel.timerState.value.remainingMillis)

    // Reset back to preset list
    composeTestRule.onNodeWithTag("timer_reset_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(TimerPhase.Idle, viewModel.timerState.value.phase)
  }
}
