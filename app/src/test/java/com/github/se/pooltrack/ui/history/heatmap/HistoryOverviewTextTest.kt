package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryOverviewTextTest {

  private fun summary(current: Int = 0, longest: Int = 0) =
      HeatmapSummary(visits = 0, activeDays = 0, current, longest)

  @Test
  fun streakText_namesTheCurrentStreak_fromTwoWeeks() {
    assertEquals("2-week streak", streakText(summary(current = 2, longest = 2)))
    assertEquals("4-week streak", streakText(summary(current = 4, longest = 9)))
  }

  @Test
  fun streakText_isNull_withoutAStreak() {
    assertNull(streakText(summary(current = 1, longest = 5)))
    assertNull(streakText(summary()))
  }

  @Test
  fun overviewSummary_countsTheWeek_andAddsTheStreak() {
    assertEquals("5 this week · 4-week streak", overviewSummary(5, summary(current = 4)))
  }

  @Test
  fun overviewSummary_isJustTheWeek_withoutAStreak() {
    assertEquals("0 this week", overviewSummary(0, summary()))
    assertEquals("1 this week", overviewSummary(1, summary(current = 1)))
  }
}
