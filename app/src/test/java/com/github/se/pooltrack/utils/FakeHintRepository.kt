package com.github.se.pooltrack.utils

import com.github.se.pooltrack.model.hint.Hint
import com.github.se.pooltrack.model.hint.HintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** [HintRepository] for unit tests: keeps the dismissed hints in memory. */
class FakeHintRepository(dismissed: Set<Hint> = emptySet()) : HintRepository {

  private val state = MutableStateFlow(dismissed)

  /** The hints dismissed so far. */
  val dismissed: Set<Hint>
    get() = state.value

  override fun isDismissed(hint: Hint): Flow<Boolean> = state.map { hint in it }

  override suspend fun dismiss(hint: Hint) {
    state.value = state.value + hint
  }
}
