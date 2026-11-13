package com.github.se.pooltrack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class LaneRopeTest {

  @Test
  fun laneFloatAt_putsRedFloatsAtBothEnds() {
    val rope = (0 until 10).map { laneFloatAt(it, 10) }

    assertEquals(List(3) { LaneFloat.End }, rope.take(3))
    assertEquals(List(3) { LaneFloat.End }, rope.takeLast(3))
  }

  @Test
  fun laneFloatAt_alternatesBlueAndWhiteInTheMiddle() {
    val middle = (3 until 7).map { laneFloatAt(it, 10) }

    assertEquals(listOf(LaneFloat.White, LaneFloat.Blue, LaneFloat.White, LaneFloat.Blue), middle)
  }

  @Test
  fun laneFloatColors_givesEachKindItsOwnColors() {
    assertEquals(PoolPalette.FloatRed, laneFloatColors(LaneFloat.End))
    assertEquals(PoolPalette.FloatBlue, laneFloatColors(LaneFloat.Blue))
    assertEquals(PoolPalette.FloatWhite, laneFloatColors(LaneFloat.White))
  }
}
