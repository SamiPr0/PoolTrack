package com.github.se.pooltrack.ui.subscription

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionSharedUiTest {

  @get:Rule val composeRule = createComposeRule()

  @Composable
  private fun CooldownProbe(lastEntry: Instant?, onValue: (Duration?) -> Unit) {
    val remaining = rememberRemainingCooldown(lastEntry)
    onValue(remaining)
    Text(remaining?.let { formatCooldown(it) } ?: "open", modifier = Modifier.testTag("cooldown"))
  }

  @Test
  fun formatCooldown_showsHoursAndMinutes_whenBothArePresent() {
    assertEquals("2h 30m", formatCooldown(Duration.ofMinutes(150)))
  }

  @Test
  fun formatCooldown_showsOnlyHours_whenMinutesAreZero() {
    assertEquals("2h", formatCooldown(Duration.ofHours(2).plusSeconds(59)))
  }

  @Test
  fun formatCooldown_showsOnlyMinutes_whenUnderAnHour() {
    assertEquals("45m", formatCooldown(Duration.ofMinutes(45)))
  }

  @Test
  fun formatCooldown_showsOneMinute_atExactlyOneMinute() {
    assertEquals("1m", formatCooldown(Duration.ofMinutes(1)))
  }

  @Test
  fun formatCooldown_showsLessThanAMinute_whenUnderOneMinute() {
    assertEquals("less than a minute", formatCooldown(Duration.ofSeconds(59)))
  }

  @Test
  fun formatCooldown_showsLessThanAMinute_whenZero() {
    assertEquals("less than a minute", formatCooldown(Duration.ZERO))
  }

  @Test
  fun rememberRemainingCooldown_isNull_whenThereIsNoEntry() {
    var value: Duration? = Duration.ZERO
    composeRule.setContent { CooldownProbe(null) { value = it } }

    assertNull(value)
    composeRule.onNodeWithTag("cooldown").assertTextEquals("open")
  }

  @Test
  fun rememberRemainingCooldown_isNull_whenTheCooldownHasElapsed() {
    var value: Duration? = Duration.ZERO
    val entry = Instant.now().minus(Duration.ofHours(5)).minusSeconds(60)
    composeRule.setContent { CooldownProbe(entry) { value = it } }

    assertNull(value)
  }

  @Test
  fun rememberRemainingCooldown_isTheTimeLeft_whenEnteredRecently() {
    var value: Duration? = null
    val entry = Instant.now().minus(Duration.ofHours(1))
    composeRule.setContent { CooldownProbe(entry) { value = it } }

    val remaining = value
    assertNotNull(remaining)
    // About 4h left; a few milliseconds of real time may already have elapsed.
    assertTrue(remaining!! <= Duration.ofHours(4))
    assertTrue(remaining > Duration.ofHours(4).minusSeconds(30))
    composeRule.onNodeWithTag("cooldown").assertTextEquals("3h 59m")
  }

  @Test
  fun rememberRemainingCooldown_followsTheEntry_whenItChanges() {
    var entry by mutableStateOf<Instant?>(null)
    var value: Duration? = Duration.ZERO
    composeRule.setContent { CooldownProbe(entry) { value = it } }
    assertNull(value)

    entry = Instant.now().minus(Duration.ofMinutes(1))
    composeRule.waitForIdle()

    assertNotNull(value)
    composeRule.onNodeWithTag("cooldown").assertTextEquals("4h 59m")
  }

  @Test
  fun rememberRemainingCooldown_keepsTheCooldown_afterTheRefreshInterval() {
    var value: Duration? = null
    val entry = Instant.now().minus(Duration.ofHours(1))
    composeRule.setContent { CooldownProbe(entry) { value = it } }

    // The refresh loop re-reads the clock every 30s; the cooldown is hours long, so it stays.
    composeRule.mainClock.advanceTimeBy(31_000)
    composeRule.waitForIdle()

    assertNotNull(value)
    composeRule.onNodeWithTag("cooldown").assertTextEquals("3h 59m")
  }
}
