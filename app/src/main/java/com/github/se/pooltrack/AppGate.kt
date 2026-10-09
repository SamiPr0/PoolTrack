package com.github.se.pooltrack

import com.github.se.pooltrack.ui.poolstay.PoolStayState

/** The screen that takes over the app, or [App] when none does. */
enum class AppGate {
  /** Waiting for the stored entries, so nothing is shown yet (a blank frame, not a flash). */
  Loading,

  /** The user has to log how far they swam, see `PoolStayScreen`. */
  PoolStay,

  /** A newer release has to be installed, see `UpdateRequiredScreen`. */
  Update,

  /** Nobody is signed in with a real account. */
  SignIn,

  /** The normal app. */
  App,
}

/**
 * Which screen takes over the app, in order of priority.
 *
 * A swim waiting for its distance comes before the mandatory update: the update replaces the whole
 * app and can take a while on the weak signal of a pool, and the user must not be kept from logging
 * a swim they just finished. The distance is stored with the entry, so the update is simply offered
 * right after it is logged.
 *
 * @param updateAvailable Whether a newer release has to be installed.
 * @param signedIn Whether a real (non-anonymous) account is signed in; entries, and so swims, only
 *   exist for it.
 * @param poolStay Where the user's current stay in the pool stands.
 */
fun appGate(updateAvailable: Boolean, signedIn: Boolean, poolStay: PoolStayState): AppGate =
    when {
      signedIn && poolStay.needsAttention() -> AppGate.PoolStay
      // Not known yet whether a swim is waiting: show nothing rather than the update for a moment.
      signedIn && poolStay == PoolStayState.Loading -> AppGate.Loading
      updateAvailable -> AppGate.Update
      !signedIn -> AppGate.SignIn
      else -> AppGate.App
    }

private fun PoolStayState.needsAttention() =
    this is PoolStayState.CancelWindow || this is PoolStayState.LogRequired
