package com.github.se.pooltrack.ui.history.heatmap

import com.github.se.pooltrack.model.entry.Entry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class HeatmapModelTest {

  // Wednesday 2026-10-07.
  private val today = LocalDate.of(2026, 10, 7)

  private fun build(counts: Map<LocalDate, Int> = emptyMap(), weeks: Int = 5) =
      buildHeatmap(counts, today, weeks, DayOfWeek.MONDAY)

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

  @Test
  fun buildHeatmap_hasOneColumnPerWeekOfSevenSlots() {
    val heatmap = build(weeks = 5)

    assertEquals(5, heatmap.weeks.size)
    assertEquals(listOf(7, 7, 7, 7, 7), heatmap.weeks.map { it.size })
  }

  @Test
  fun buildHeatmap_endsWithTheWeekOfToday_andHidesTheFuture() {
    val lastWeek = build().weeks.last()

    // Monday..Wednesday exist, Thursday..Sunday are in the future.
    assertEquals(LocalDate.of(2026, 10, 5), lastWeek[0]?.date)
    assertEquals(today, lastWeek[2]?.date)
    assertNull(lastWeek[3])
    assertNull(lastWeek[6])
  }

  @Test
  fun buildHeatmap_startsColumnsOnTheGivenFirstDayOfWeek() {
    val sundayFirst = buildHeatmap(emptyMap(), today, weekCount = 3, DayOfWeek.SUNDAY)

    assertEquals(LocalDate.of(2026, 10, 4), sundayFirst.weeks.last()[0]?.date)
    assertEquals(LocalDate.of(2026, 9, 20), sundayFirst.weeks.first()[0]?.date)
  }

  @Test
  fun buildHeatmap_assignsCountAndLevelToEachDay() {
    val monday = LocalDate.of(2026, 10, 5)
    val heatmap = build(mapOf(monday to 2))

    val cell = heatmap.weeks.last()[0]
    assertNotNull(cell)
    assertEquals(2, cell!!.count)
    assertEquals(2, cell.level)
    assertEquals(0, heatmap.weeks.last()[1]?.count)
    assertEquals(0, heatmap.weeks.last()[1]?.level)
  }

  @Test
  fun buildHeatmap_ignoresEntriesOutsideTheRange() {
    val tooOld = LocalDate.of(2020, 1, 1)
    val heatmap = build(mapOf(tooOld to 50))

    assertEquals(0, summarize(heatmap).visits)
    // The ancient busy day must not flatten the scale of the visible range.
    assertEquals(1, build(mapOf(tooOld to 50, today to 1)).weeks.last()[2]?.level)
  }

  @Test
  fun buildHeatmap_rejectsNonPositiveWeekCount() {
    assertThrows(IllegalArgumentException::class.java) { build(weeks = 0) }
  }

  @Test
  fun summarize_countsVisitsAndActiveDays() {
    val counts =
        mapOf(
            LocalDate.of(2026, 10, 5) to 2,
            LocalDate.of(2026, 10, 6) to 1,
            LocalDate.of(2026, 9, 1) to 1,
        )

    val summary = summarize(build(counts, weeks = 8))

    assertEquals(4, summary.visits)
    assertEquals(3, summary.activeDays)
  }

  @Test
  fun summarize_ofAnEmptyHeatmap_isAllZero() {
    assertEquals(HeatmapSummary(0, 0, 0, 0), summarize(build()))
  }

  @Test
  fun summarize_countsConsecutiveWeeksAsAStreak() {
    // Weeks starting 09-21, 09-28 and 10-05 all have a visit.
    val counts =
        mapOf(
            LocalDate.of(2026, 9, 22) to 1,
            LocalDate.of(2026, 9, 30) to 1,
            LocalDate.of(2026, 10, 5) to 1,
        )

    val summary = summarize(build(counts))

    assertEquals(3, summary.currentWeekStreak)
    assertEquals(3, summary.longestWeekStreak)
  }

  @Test
  fun summarize_keepsTheStreakAlive_whenThisWeekHasNoVisitYet() {
    val counts = mapOf(LocalDate.of(2026, 9, 29) to 1, LocalDate.of(2026, 9, 22) to 1)

    val summary = summarize(build(counts))

    assertEquals(2, summary.currentWeekStreak)
  }

  @Test
  fun summarize_resetsTheCurrentStreak_afterAMissedWeek() {
    // Visits two and three weeks ago, none last week or this week.
    val counts = mapOf(LocalDate.of(2026, 9, 22) to 1, LocalDate.of(2026, 9, 15) to 1)

    val summary = summarize(build(counts))

    assertEquals(0, summary.currentWeekStreak)
    assertEquals(2, summary.longestWeekStreak)
  }

  @Test
  fun summarize_worksWithASingleWeek() {
    val summary = summarize(build(mapOf(today to 1), weeks = 1))

    assertEquals(1, summary.currentWeekStreak)
    assertEquals(1, summary.longestWeekStreak)
  }
}
