package com.github.se.pooltrack.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepository
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel for the Home screen. Exposes stats derived from the confirmed entries. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: EntryRepository = EntryRepositoryLocal(application)

  val stats: StateFlow<HomeStats> =
      repository
          .getEntries()
          .map { entries: List<Entry> -> computeHomeStats(entries) }
          .stateIn(viewModelScope, SharingStarted.Eagerly, computeHomeStats(emptyList()))
}
