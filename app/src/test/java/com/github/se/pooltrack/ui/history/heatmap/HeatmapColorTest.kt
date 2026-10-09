package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapColorTest {

  @Test
  fun levelAlpha_startsAtTheMinimum_andEndsOpaque() {
    assertEquals(0.7f, levelAlpha(1, DAY_MIN_ALPHA), 0.0001f)
    assertEquals(1f, levelAlpha(MAX_LEVEL, DAY_MIN_ALPHA), 0.0001f)
  }

  @Test
  fun levelAlpha_growsWithTheLevel() {
    val alphas = (1..MAX_LEVEL).map { levelAlpha(it, YEAR_MIN_ALPHA) }

    assertEquals(alphas.sorted(), alphas)
    assertEquals(alphas.size, alphas.distinct().size)
  }

  @Test
  fun levelAlpha_clampsLevelsOutsideTheScale() {
    assertEquals(levelAlpha(1, DAY_MIN_ALPHA), levelAlpha(0, DAY_MIN_ALPHA), 0.0001f)
    assertEquals(levelAlpha(MAX_LEVEL, DAY_MIN_ALPHA), levelAlpha(9, DAY_MIN_ALPHA), 0.0001f)
  }

  @Test
  fun aSingleVisitIsClearlyFilled_butTheYearViewStillSpreadsOut() {
    assertTrue(levelAlpha(1, DAY_MIN_ALPHA) >= 0.6f)
    assertTrue(levelAlpha(MAX_LEVEL, YEAR_MIN_ALPHA) - levelAlpha(1, YEAR_MIN_ALPHA) >= 0.6f)
  }
}
