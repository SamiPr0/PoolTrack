package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryOverviewTextTest {

  private fun summary(visits: Int = 0, current: Int = 0, longest: Int = 0) =
      HeatmapSummary(visits, activeDays = visits, current, longest)

  @Test
  fun summaryTitle_isSingular_forOneVisit() {
    assertEquals("1 visit in the last year", summaryTitle(summary(visits = 1)))
  }

  @Test
  fun summaryTitle_isPlural_otherwise() {
    assertEquals("0 visits in the last year", summaryTitle(summary(visits = 0)))
    assertEquals("34 visits in the last year", summaryTitle(summary(visits = 34)))
  }

  @Test
  fun streakText_showsTheCurrentStreak_fromTwoWeeks() {
    assertEquals("3-week streak (best: 5)", streakText(summary(current = 3, longest = 5)))
  }

  @Test
  fun streakText_showsTheBestStreak_whenTheCurrentOneIsOver() {
    assertEquals("Best streak: 4 weeks in a row", streakText(summary(current = 1, longest = 4)))
  }

  @Test
  fun streakText_isNull_withoutAStreak() {
    assertNull(streakText(summary(current = 1, longest = 1)))
    assertNull(streakText(summary()))
  }
}
