package com.github.se.pooltrack.ui.history

import com.github.se.pooltrack.ui.history.heatmap.visitsLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryListItemsTest {

  // Wednesday 2026-10-07; with Monday first, this week started on 2026-10-05.
  private val today = LocalDate.of(2026, 10, 7)
  private val monday = DayOfWeek.MONDAY

  private fun label(weekStart: LocalDate, first: DayOfWeek = monday, now: LocalDate = today) =
      weekLabel(weekStart, now, first, Locale.ENGLISH)

  @Test
  fun weekStart_isTheFirstDayOfWeekOnOrBefore() {
    assertEquals(LocalDate.of(2026, 10, 5), weekStart(today, monday))
    assertEquals(LocalDate.of(2026, 10, 5), weekStart(LocalDate.of(2026, 10, 5), monday))
    assertEquals(LocalDate.of(2026, 10, 5), weekStart(LocalDate.of(2026, 10, 11), monday))
    assertEquals(LocalDate.of(2026, 10, 4), weekStart(today, DayOfWeek.SUNDAY))
  }

  @Test
  fun weekLabel_namesThisWeekAndLastWeek() {
    assertEquals("This week", label(LocalDate.of(2026, 10, 5)))
    assertEquals("Last week", label(LocalDate.of(2026, 9, 28)))
  }

  @Test
  fun weekLabel_showsTheDayRange_withinAMonth() {
    assertEquals("Sep 14 – 20", label(LocalDate.of(2026, 9, 14)))
  }

  @Test
  fun weekLabel_showsBothMonths_whenTheWeekCrossesOne() {
    assertEquals("Aug 31 – Sep 6", label(LocalDate.of(2026, 8, 31)))
  }

  @Test
  fun weekLabel_addsTheYear_forOtherYears() {
    assertEquals("Oct 6 – 12, 2025", label(LocalDate.of(2025, 10, 6)))
  }

  @Test
  fun weekLabel_showsBothYears_whenTheWeekSpansNewYear() {
    assertEquals("Dec 28, 2025 – Jan 3, 2026", label(LocalDate.of(2025, 12, 28), now = today))
  }

  @Test
  fun weekLabel_followsTheFirstDayOfWeek() {
    // With Sunday first, "this week" began on Sunday 2026-10-04.
    assertEquals("This week", label(LocalDate.of(2026, 10, 4), DayOfWeek.SUNDAY))
    assertEquals("Last week", label(LocalDate.of(2026, 9, 27), DayOfWeek.SUNDAY))
  }

  @Test
  fun visitsLabel_isSingularOnlyForOne() {
    assertEquals("1 visit", visitsLabel(1))
    assertEquals("2 visits", visitsLabel(2))
    assertEquals("0 visits", visitsLabel(0))
  }
}
