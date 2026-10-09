package com.github.se.pooltrack.ui.history.heatmap

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YearModelTest {

  private val today = LocalDate.of(2026, 10, 7)

  @Test
  fun monthCounts_sumsDaysPerMonth() {
    val counts =
        mapOf(
            LocalDate.of(2026, 10, 1) to 1,
            LocalDate.of(2026, 10, 5) to 2,
            LocalDate.of(2026, 9, 30) to 1,
        )

    assertEquals(
        mapOf(YearMonth.of(2026, 10) to 3, YearMonth.of(2026, 9) to 1),
        monthCounts(counts),
    )
  }

  @Test
  fun buildYearTiles_hasTwelveMonthsJanuaryFirst() {
    val tiles = buildYearTiles(2026, emptyMap(), today)

    assertEquals((1..12).map { YearMonth.of(2026, it) }, tiles.map { it.month })
  }

  @Test
  fun buildYearTiles_marksMonthsAfterTodayAsFuture() {
    val tiles = buildYearTiles(2026, emptyMap(), today).associateBy { it.month.monthValue }

    assertFalse(tiles.getValue(10).isFuture)
    assertTrue(tiles.getValue(11).isFuture)
    assertFalse(buildYearTiles(2025, emptyMap(), today).any { it.isFuture })
  }

  @Test
  fun buildYearTiles_shadesMonthsByTheirVisits() {
    val counts =
        mapOf(YearMonth.of(2026, 1) to 0, YearMonth.of(2026, 2) to 4, YearMonth.of(2026, 3) to 12)

    val tiles = buildYearTiles(2026, counts, today).associateBy { it.month.monthValue }

    assertEquals(0, tiles.getValue(1).level)
    assertEquals(4, tiles.getValue(2).count)
    assertEquals(MAX_LEVEL, tiles.getValue(3).level)
    assertTrue(tiles.getValue(2).level in 1 until MAX_LEVEL)
  }

  @Test
  fun buildYearTiles_scalesAgainstTheBusiestMonthOnceItBeatsTheFloor() {
    val counts = mapOf(YearMonth.of(2026, 1) to 30, YearMonth.of(2026, 2) to 12)

    val tiles = buildYearTiles(2026, counts, today).associateBy { it.month.monthValue }

    assertEquals(MAX_LEVEL, tiles.getValue(1).level)
    assertTrue(tiles.getValue(2).level < MAX_LEVEL)
  }

  @Test
  fun yearsBetween_listsEveryYear() {
    assertEquals(listOf(2024, 2025, 2026), yearsBetween(2024, 2026))
    assertEquals(listOf(2026), yearsBetween(2026, 2026))
    assertEquals(listOf(2026), yearsBetween(2027, 2026))
  }

  @Test
  fun monthToFocus_keepsTheCurrentMonth_whenItIsInThatYear() {
    val months = listOf(YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1))

    assertEquals(
        YearMonth.of(2026, 1),
        monthToFocus(YearMonth.of(2026, 1), 2026, months),
    )
  }

  @Test
  fun monthToFocus_picksTheLatestMonthOfTheNewYear() {
    val months = listOf(YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1))

    assertEquals(YearMonth.of(2025, 12), monthToFocus(YearMonth.of(2026, 1), 2025, months))
  }

  @Test
  fun monthToFocus_keepsTheCurrentMonth_whenTheYearHasNoMonths() {
    assertEquals(
        YearMonth.of(2026, 1),
        monthToFocus(YearMonth.of(2026, 1), 2020, listOf(YearMonth.of(2026, 1))),
    )
  }
}
