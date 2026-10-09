package com.github.se.pooltrack.ui.history.heatmap

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthModelTest {

  private val october = YearMonth.of(2026, 10) // 1st is a Thursday, 31 days
  private val today = LocalDate.of(2026, 10, 7)

  private fun grid(
      counts: Map<LocalDate, Int> = emptyMap(),
      month: YearMonth = october,
      firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
  ) = buildMonthGrid(month, counts, today, firstDayOfWeek)

  @Test
  fun buildMonthGrid_padsEveryWeekToSevenSlots() {
    assertEquals(listOf(7, 7, 7, 7, 7), grid().weeks.map { it.size })
  }

  @Test
  fun buildMonthGrid_leavesBlanksBeforeTheFirst_accordingToFirstDayOfWeek() {
    val mondayFirst = grid().weeks.first()
    assertEquals(listOf(null, null, null), mondayFirst.take(3))
    assertEquals(october.atDay(1), mondayFirst[3]?.date)

    val sundayFirst = grid(firstDayOfWeek = DayOfWeek.SUNDAY).weeks.first()
    assertEquals(4, sundayFirst.indexOfFirst { it != null })
  }

  @Test
  fun buildMonthGrid_startsRightAway_whenTheFirstIsTheFirstWeekday() {
    // 2026-06-01 is a Monday.
    val june = grid(month = YearMonth.of(2026, 6))
    assertEquals(LocalDate.of(2026, 6, 1), june.weeks.first().first()?.date)
  }

  @Test
  fun buildMonthGrid_containsEveryDayOnce_inOrder() {
    val dates = grid().weeks.flatten().filterNotNull().map { it.date }

    assertEquals((1..31).map { october.atDay(it) }, dates)
  }

  @Test
  fun buildMonthGrid_leavesBlanksAfterTheLastDay() {
    // 31 October is a Saturday, so with Monday first the last row has one blank (Sunday).
    val lastWeek = grid().weeks.last()
    assertEquals(october.atEndOfMonth(), lastWeek[5]?.date)
    assertNull(lastWeek[6])
  }

  @Test
  fun buildMonthGrid_needsFourWeeks_forAFebruaryThatFitsExactly() {
    // 2027-02 has 28 days and starts on a Monday.
    assertEquals(4, grid(month = YearMonth.of(2027, 2)).weeks.size)
  }

  @Test
  fun buildMonthGrid_marksTodayAndTheFuture() {
    val days = grid().weeks.flatten().filterNotNull().associateBy { it.date.dayOfMonth }

    assertEquals(true, days.getValue(7).isToday)
    assertEquals(false, days.getValue(6).isToday)
    assertEquals(false, days.getValue(7).isFuture)
    assertEquals(true, days.getValue(8).isFuture)
    assertEquals(false, days.getValue(6).isFuture)
  }

  @Test
  fun buildMonthGrid_assignsCountAndLevel() {
    val day = LocalDate.of(2026, 10, 5)
    val days = grid(mapOf(day to 2)).weeks.flatten().filterNotNull().associateBy { it.date }

    assertEquals(2, days.getValue(day).count)
    assertEquals(2, days.getValue(day).level)
    assertEquals(0, days.getValue(day.plusDays(1)).level)
  }

  @Test
  fun buildMonthGrid_scalesAgainstTheGivenMaxCount() {
    val day = LocalDate.of(2026, 10, 5)
    val grid = buildMonthGrid(october, mapOf(day to 2), today, maxCount = 8)

    assertEquals(1, grid.weeks.flatten().filterNotNull().first { it.date == day }.level)
  }

  @Test
  fun monthGrid_visits_sumsTheMonthOnly() {
    val counts =
        mapOf(
            LocalDate.of(2026, 10, 1) to 1,
            LocalDate.of(2026, 10, 5) to 2,
            LocalDate.of(2026, 9, 30) to 5,
        )

    assertEquals(3, grid(counts).visits)
    assertEquals(0, grid().visits)
  }

  @Test
  fun monthsBetween_listsEveryMonthOldestFirst() {
    val months = monthsBetween(YearMonth.of(2025, 11), YearMonth.of(2026, 2))

    assertEquals(
        listOf(
            YearMonth.of(2025, 11),
            YearMonth.of(2025, 12),
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 2),
        ),
        months,
    )
  }

  @Test
  fun monthsBetween_returnsASingleMonth_whenBothAreEqual() {
    assertEquals(listOf(october), monthsBetween(october, october))
  }

  @Test
  fun monthsBetween_fallsBackToTheLastMonth_whenFirstIsAfterIt() {
    assertEquals(listOf(october), monthsBetween(october.plusMonths(2), october))
  }

  @Test
  fun buildWeekStrip_hasTheSevenDaysOfTodaysWeek() {
    val strip = buildWeekStrip(today, emptyMap(), DayOfWeek.MONDAY)

    assertEquals((5..11).map { LocalDate.of(2026, 10, it) }, strip.map { it.date })
  }

  @Test
  fun buildWeekStrip_startsOnTheGivenFirstDayOfWeek() {
    val strip = buildWeekStrip(today, emptyMap(), DayOfWeek.SUNDAY)

    assertEquals(LocalDate.of(2026, 10, 4), strip.first().date)
  }

  @Test
  fun buildWeekStrip_marksTodayAndTheFuture_andCarriesTheCounts() {
    val monday = LocalDate.of(2026, 10, 5)
    val strip = buildWeekStrip(today, mapOf(monday to 2), DayOfWeek.MONDAY).associateBy { it.date }

    assertEquals(2, strip.getValue(monday).count)
    assertEquals(2, strip.getValue(monday).level)
    assertEquals(true, strip.getValue(today).isToday)
    assertEquals(false, strip.getValue(today).isFuture)
    assertEquals(true, strip.getValue(today.plusDays(1)).isFuture)
  }
}
