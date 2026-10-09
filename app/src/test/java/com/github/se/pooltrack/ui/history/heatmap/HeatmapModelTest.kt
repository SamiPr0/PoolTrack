package com.github.se.pooltrack.ui.history.heatmap

import com.github.se.pooltrack.model.entry.Entry
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapModelTest {

  @Test
  fun intensityLevel_isZero_withoutEntries() {
    assertEquals(0, intensityLevel(count = 0, maxCount = 10))
  }

  @Test
  fun intensityLevel_isAbsolute_untilTheScaleFloor() {
    assertEquals(listOf(1, 2, 3, 4), (1..4).map { intensityLevel(it, maxCount = 4) })
    // A single visit stays light even when it is the busiest day.
    assertEquals(1, intensityLevel(count = 1, maxCount = 1))
  }

  @Test
  fun intensityLevel_stretches_whenBusierDaysExist() {
    assertEquals(4, intensityLevel(count = 8, maxCount = 8))
    assertEquals(2, intensityLevel(count = 4, maxCount = 8))
    assertEquals(1, intensityLevel(count = 1, maxCount = 8))
  }

  @Test
  fun entryCountsByDay_groupsEntriesOfTheSameDay() {
    val zone = ZoneId.of("UTC")
    val entries =
        listOf(
            Entry(LocalDate.of(2026, 10, 5).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()),
            Entry(LocalDate.of(2026, 10, 5).atTime(18, 0).atZone(zone).toInstant().toEpochMilli()),
            Entry(LocalDate.of(2026, 10, 6).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()),
        )

    val counts = entryCountsByDay(entries, zone)

    assertEquals(mapOf(LocalDate.of(2026, 10, 5) to 2, LocalDate.of(2026, 10, 6) to 1), counts)
  }
}
