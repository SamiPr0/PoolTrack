package com.github.se.pooltrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen. Exposes every confirmed entry, most recent first.
 *
 * @property repository The repository used to read and delete entries.
 */
class HistoryViewModel(
    private val repository: EntryRepository = EntryRepositoryProvider.repository,
) : ViewModel() {

  val entries: StateFlow<List<Entry>> =
      repository.getEntries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  /** Removes [entry], e.g. to undo a misclick on the scanner confirmation. */
  fun onDeleteEntry(entry: Entry) {
    viewModelScope.launch { repository.deleteEntry(entry) }
  }
}
