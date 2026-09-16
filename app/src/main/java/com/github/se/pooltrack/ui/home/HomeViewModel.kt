package com.github.se.pooltrack.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the Home screen. Exposes stats derived from the confirmed entries, and when the
 * user last entered the pool (so Home can gray out its "open subscription" shortcut during the
 * reopen cooldown, same rule the Subscription screen itself enforces).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val entryRepository: EntryRepository = EntryRepositoryLocal(application)

  val stats: StateFlow<HomeStats> =
      entryRepository
          .getEntries()
          .map { entries: List<Entry> -> computeHomeStats(entries) }
          .stateIn(viewModelScope, SharingStarted.Eagerly, computeHomeStats(emptyList()))

  val lastEntryTimestamp: StateFlow<Instant?> =
      entryRepository
          .getLastEntryTimestamp()
          .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
