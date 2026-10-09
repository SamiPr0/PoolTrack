package com.github.se.pooltrack.ui.history.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapColorTest {

  @Test
  fun levelAlpha_runsFromTheMinimumToTheMaximum() {
    assertEquals(DayPalette.minAlpha, levelAlpha(1, DayPalette), 0.0001f)
    assertEquals(DayPalette.maxAlpha, levelAlpha(MAX_LEVEL, DayPalette), 0.0001f)
  }

  @Test
  fun levelAlpha_growsWithTheLevel() {
    val alphas = (1..MAX_LEVEL).map { levelAlpha(it, YearPalette) }

    assertEquals(alphas.sorted(), alphas)
    assertEquals(alphas.size, alphas.distinct().size)
  }

  @Test
  fun levelAlpha_clampsLevelsOutsideTheScale() {
    assertEquals(levelAlpha(1, DayPalette), levelAlpha(0, DayPalette), 0.0001f)
    assertEquals(levelAlpha(MAX_LEVEL, DayPalette), levelAlpha(9, DayPalette), 0.0001f)
  }

  @Test
  fun everyFillIsASoftTint_neverTheFullColor() {
    listOf(DayPalette, YearPalette).forEach { palette ->
      assertTrue(palette.maxAlpha <= 0.7f)
      assertTrue(palette.minAlpha > 0f)
    }
  }

  @Test
  fun aSingleVisitIsVisible_andTheYearViewStillSpreadsOut() {
    assertTrue(DayPalette.minAlpha >= 0.35f)
    assertTrue(YearPalette.maxAlpha - YearPalette.minAlpha >= 0.4f)
  }
}
