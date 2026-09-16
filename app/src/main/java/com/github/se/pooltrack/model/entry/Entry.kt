package com.github.se.pooltrack.model.entry

import java.time.Instant

/** Represents a single confirmed pool entry, i.e. a scan the scanner accepted. */
data class Entry(val timestamp: Instant)
