package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryOverviewTextTest {

  @Test
  fun overviewSummary_countsTheVisitsOfTheWeek() {
    assertEquals("0 this week", overviewSummary(0))
    assertEquals("1 this week", overviewSummary(1))
    assertEquals("5 this week", overviewSummary(5))
  }
}
