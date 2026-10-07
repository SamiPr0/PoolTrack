package com.github.se.pooltrack.ui.poolstay

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.swim.CANCEL_WINDOW
import com.github.se.pooltrack.utils.FakeEntryRepository
import com.github.se.pooltrack.utils.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PoolStayViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val enteredAt = Instant.parse("2026-10-05T10:00:00Z")
  private val waiting =
      Entry(enteredAt.toEpochMilli(), subscriptionId = "a", awaitingDistance = true)

  private var now = enteredAt
  private val ticker = MutableSharedFlow<Unit>(replay = 1)

  private fun viewModel(repository: FakeEntryRepository): PoolStayViewModel {
    ticker.tryEmit(Unit)
    return PoolStayViewModel(repository, clock = { now }, ticker = ticker)
  }

  private fun advanceTo(instant: Instant) {
    now = instant
    ticker.tryEmit(Unit)
  }

  @Test
  fun state_isNone_withoutEntries() {
    assertEquals(PoolStayState.None, viewModel(FakeEntryRepository()).state.value)
  }

  @Test
  fun state_isNone_forEntriesNotAwaitingADistance() {
    // Entries added by hand, imported or recorded before distances existed.
    val old = waiting.copy(awaitingDistance = false)
    val logged = waiting.copy(awaitingDistance = false, swimDistanceMeters = 700)

    assertEquals(PoolStayState.None, viewModel(FakeEntryRepository(listOf(old))).state.value)
    assertEquals(PoolStayState.None, viewModel(FakeEntryRepository(listOf(logged))).state.value)
  }

  @Test
  fun state_isLoading_untilTheEntriesAreKnown() {
    val neverEmits = MutableSharedFlow<Unit>()

    val viewModel = PoolStayViewModel(FakeEntryRepository(), clock = { now }, ticker = neverEmits)

    assertEquals(PoolStayState.Loading, viewModel.state.value)
  }

  @Test
  fun state_isTheCancelWindow_rightAfterTheScan() {
    val state = viewModel(FakeEntryRepository(listOf(waiting))).state.value

    assertEquals(PoolStayState.CancelWindow(waiting, Duration.ZERO, CANCEL_WINDOW), state)
  }

  @Test
  fun state_countsDownAndSwitchesToLogRequired_atTenMinutes() {
    val viewModel = viewModel(FakeEntryRepository(listOf(waiting)))

    advanceTo(enteredAt.plus(Duration.ofMinutes(9)).plusSeconds(59))
    assertEquals(
        PoolStayState.CancelWindow(waiting, Duration.ofSeconds(599), Duration.ofSeconds(1)),
        viewModel.state.value,
    )

    advanceTo(enteredAt.plus(CANCEL_WINDOW))
    assertEquals(PoolStayState.LogRequired(waiting), viewModel.state.value)
  }

  @Test
  fun state_isLogRequiredAtOnce_onAColdStartLongAfterTheScan() {
    now = enteredAt.plus(Duration.ofHours(5))

    val state = viewModel(FakeEntryRepository(listOf(waiting))).state.value

    assertEquals(PoolStayState.LogRequired(waiting), state)
  }

  @Test
  fun state_isTheCancelWindowAgain_onAColdStartInsideIt() {
    now = enteredAt.plus(Duration.ofMinutes(4))

    val state = viewModel(FakeEntryRepository(listOf(waiting))).state.value

    assertEquals(
        PoolStayState.CancelWindow(waiting, Duration.ofMinutes(4), Duration.ofMinutes(6)),
        state,
    )
  }

  @Test
  fun state_onlyLooksAtTheLatestEntry() {
    val older =
        waiting.copy(timestampEpochMilli = enteredAt.minus(Duration.ofDays(1)).toEpochMilli())
    now = enteredAt.plus(Duration.ofHours(1))

    val viewModel =
        viewModel(FakeEntryRepository(listOf(older, waiting.copy(awaitingDistance = false))))

    assertEquals(PoolStayState.None, viewModel.state.value)
  }

  @Test
  fun state_followsTheRepository_whenAnEntryIsAdded() = runTest {
    val repository = FakeEntryRepository()
    val viewModel = viewModel(repository)

    repository.addEntry(waiting)

    assertTrue(viewModel.state.value is PoolStayState.CancelWindow)
  }

  @Test
  fun onCancelEntry_removesTheEntryAndLeavesTheStay_insideTheCancelWindow() {
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)

    viewModel.onCancelEntry()

    assertEquals(emptyList<Entry>(), repository.storedEntries)
    assertEquals(PoolStayState.None, viewModel.state.value)
  }

  @Test
  fun onCancelEntry_leavesOlderEntriesAlone() {
    val older = Entry(enteredAt.minus(Duration.ofDays(2)).toEpochMilli(), subscriptionId = "a")
    val repository = FakeEntryRepository(listOf(older, waiting))
    val viewModel = viewModel(repository)

    viewModel.onCancelEntry()

    assertEquals(listOf(older), repository.storedEntries)
  }

  @Test
  fun onCancelEntry_isIgnored_onceTheWindowIsOver() {
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)
    // The screen still shows the old state for up to a second: the clock decides.
    now = enteredAt.plus(CANCEL_WINDOW)

    viewModel.onCancelEntry()

    assertEquals(listOf(waiting), repository.storedEntries)
  }

  @Test
  fun onCancelEntry_isIgnored_whenTheDistanceIsRequired() {
    now = enteredAt.plus(Duration.ofHours(1))
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)

    viewModel.onCancelEntry()

    assertEquals(listOf(waiting), repository.storedEntries)
  }

  @Test
  fun onDistanceChanged_keepsDigitsOnly_andAtMostSix() {
    val viewModel = viewModel(FakeEntryRepository())

    viewModel.onDistanceChanged("12a.5,0")
    assertEquals("1250", viewModel.distanceInput.value)

    viewModel.onDistanceChanged("123456789")
    assertEquals("123456", viewModel.distanceInput.value)
  }

  @Test
  fun isDistanceValid_followsTheLimits() {
    val viewModel = viewModel(FakeEntryRepository())
    assertFalse(viewModel.isDistanceValid.value)

    for ((text, valid) in
        listOf("0" to false, "1" to true, "1200" to true, "50000" to true, "50001" to false)) {
      viewModel.onDistanceChanged(text)
      assertEquals(text, valid, viewModel.isDistanceValid.value)
    }
  }

  @Test
  fun onSaveDistance_storesTheDistance_clearsTheMarkerAndEndsTheStay() {
    now = enteredAt.plus(Duration.ofMinutes(45))
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)

    viewModel.onDistanceChanged("1200")
    viewModel.onSaveDistance()

    assertEquals(
        listOf(waiting.copy(swimDistanceMeters = 1200, awaitingDistance = false)),
        repository.storedEntries,
    )
    assertEquals(PoolStayState.None, viewModel.state.value)
    assertEquals("", viewModel.distanceInput.value)
  }

  @Test
  fun onSaveDistance_isIgnored_whileTheInputIsInvalid() {
    now = enteredAt.plus(Duration.ofMinutes(45))
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)

    for (text in listOf("", "0", "50001")) {
      viewModel.onDistanceChanged(text)
      viewModel.onSaveDistance()
    }

    assertEquals(listOf(waiting), repository.storedEntries)
    assertTrue(viewModel.state.value is PoolStayState.LogRequired)
  }

  @Test
  fun onSaveDistance_isIgnored_insideTheCancelWindow() {
    val repository = FakeEntryRepository(listOf(waiting))
    val viewModel = viewModel(repository)

    viewModel.onDistanceChanged("1000")
    viewModel.onSaveDistance()

    assertEquals(listOf(waiting), repository.storedEntries)
  }

  @Test
  fun computePoolStayState_usesTheEntryTimestamp_notTheViewModel() {
    val state = computePoolStayState(listOf(waiting), enteredAt.plus(Duration.ofMinutes(1)))

    assertEquals(
        PoolStayState.CancelWindow(waiting, Duration.ofMinutes(1), Duration.ofMinutes(9)),
        state,
    )
  }
}
