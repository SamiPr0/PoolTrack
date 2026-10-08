package com.github.se.pooltrack.ui.history

import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryListItemsTest {

  private val thisMonth = YearMonth.of(2026, 10)

  @Test
  fun monthLabel_omitsTheYear_forTheCurrentYear() {
    assertEquals("March", monthLabel(YearMonth.of(2026, 3), thisMonth, Locale.ENGLISH))
    assertEquals("October", monthLabel(thisMonth, thisMonth, Locale.ENGLISH))
  }

  @Test
  fun monthLabel_includesTheYear_forOtherYears() {
    assertEquals("December 2025", monthLabel(YearMonth.of(2025, 12), thisMonth, Locale.ENGLISH))
  }

  @Test
  fun visitsLabel_isSingularOnlyForOne() {
    assertEquals("1 visit", visitsLabel(1))
    assertEquals("2 visits", visitsLabel(2))
    assertEquals("0 visits", visitsLabel(0))
  }
}
