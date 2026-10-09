package com.github.se.pooltrack.ui.history.heatmap

import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.timestamp
import java.time.LocalDate
import java.time.ZoneId

/** Number of intensity steps above "no visit"; levels run from 0 (none) to [MAX_LEVEL]. */
const val MAX_LEVEL = 4

/** Counts [entries] per calendar day, resolving days in [zone]. */
fun entryCountsByDay(
    entries: List<Entry>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Int> = entries.groupingBy { it.timestamp.atZone(zone).toLocalDate() }.eachCount()

/**
 * The intensity step of a day with [count] entries. The scale is absolute (one entry is level 1,
 * two are level 2, ...) until a day beats [scaleFloor], after which it stretches so that the
 * busiest day is [MAX_LEVEL]. An absolute scale matters here: most days have a single visit, and a
 * purely relative one would paint every cell with the darkest color.
 */
fun intensityLevel(count: Int, maxCount: Int, scaleFloor: Int = MAX_LEVEL): Int {
  if (count <= 0) return 0
  val scale = maxOf(maxCount, scaleFloor)
  return ((count * MAX_LEVEL + scale - 1) / scale).coerceIn(1, MAX_LEVEL)
}
