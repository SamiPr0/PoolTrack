package com.github.se.pooltrack.model.hint

import kotlinx.coroutines.flow.Flow

/** A one-time tip the app shows until the user has seen it. */
enum class Hint {
  /** Visits in the history list can be deleted by swiping them to the left. */
  SwipeToDelete,
}

/** Represents a repository that remembers which [Hint]s the user has dealt with. */
interface HintRepository {

  /** Whether [hint] was dismissed, emitting again whenever that changes. */
  fun isDismissed(hint: Hint): Flow<Boolean>

  /** Remembers that the user dealt with [hint], so it is not shown again. */
  suspend fun dismiss(hint: Hint)
}
