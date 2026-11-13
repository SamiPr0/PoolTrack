package com.github.se.pooltrack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class PoolFloorTest {

  @Test
  fun depthLabel_writesOneDecimalAndTheUnit() {
    assertEquals("0.8 m", depthLabel(0.8))
    assertEquals("2.0 m", depthLabel(2.0))
  }

  @Test
  fun poolDepths_goFromShallowToDeep() {
    assertEquals(PoolDepths.sorted(), PoolDepths)
    assertEquals(5, PoolDepths.size)
  }
}
