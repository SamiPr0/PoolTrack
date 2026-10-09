package com.github.se.pooltrack

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.ui.poolstay.PoolStayState
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class AppGateTest {

  private val entry = Entry(timestampEpochMilli = 0L, awaitingDistance = true)
  private val cancelWindow =
      PoolStayState.CancelWindow(entry, Duration.ofMinutes(2), Duration.ofMinutes(8))
  private val logRequired = PoolStayState.LogRequired(entry)

  private fun gate(update: Boolean, signedIn: Boolean, poolStay: PoolStayState) =
      appGate(updateAvailable = update, signedIn = signedIn, poolStay = poolStay)

  @Test
  fun aSwimWaitingForItsDistance_comesBeforeTheUpdate() {
    assertEquals(AppGate.PoolStay, gate(update = true, signedIn = true, poolStay = logRequired))
    assertEquals(AppGate.PoolStay, gate(update = true, signedIn = true, poolStay = cancelWindow))
  }

  @Test
  fun aSwimWaitingForItsDistance_isShown_withoutAnUpdate() {
    assertEquals(AppGate.PoolStay, gate(update = false, signedIn = true, poolStay = logRequired))
  }

  @Test
  fun theUpdate_isShown_onceNoSwimIsWaiting() {
    assertEquals(
        AppGate.Update,
        gate(update = true, signedIn = true, poolStay = PoolStayState.None),
    )
  }

  @Test
  fun nothingIsShown_whileItIsUnknownWhetherASwimIsWaiting() {
    // So the update never flashes up for a moment before the swim screen takes over.
    assertEquals(
        AppGate.Loading,
        gate(update = true, signedIn = true, poolStay = PoolStayState.Loading),
    )
    assertEquals(
        AppGate.Loading,
        gate(update = false, signedIn = true, poolStay = PoolStayState.Loading),
    )
  }

  @Test
  fun theUpdate_isShown_toSomeoneNotSignedIn() {
    assertEquals(
        AppGate.Update,
        gate(update = true, signedIn = false, poolStay = PoolStayState.None),
    )
    assertEquals(
        AppGate.Update,
        gate(update = true, signedIn = false, poolStay = PoolStayState.Loading),
    )
  }

  @Test
  fun signIn_isShown_whenNobodyIsSignedIn_andNoUpdateIsNeeded() {
    assertEquals(
        AppGate.SignIn,
        gate(update = false, signedIn = false, poolStay = PoolStayState.None),
    )
  }

  @Test
  fun aSwimOfAnotherAccount_isIgnored_whenNobodyIsSignedIn() {
    assertEquals(AppGate.SignIn, gate(update = false, signedIn = false, poolStay = logRequired))
  }

  @Test
  fun theNormalApp_isShown_whenNothingTakesOver() {
    assertEquals(AppGate.App, gate(update = false, signedIn = true, poolStay = PoolStayState.None))
  }
}
