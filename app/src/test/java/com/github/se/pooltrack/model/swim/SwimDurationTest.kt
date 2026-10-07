package com.github.se.pooltrack.model.swim

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class SwimDurationTest {

  @Test
  fun formatSwimDuration_showsMinutesOnly_underAnHour() {
    assertEquals("45min", formatSwimDuration(Duration.ofMinutes(45)))
  }

  @Test
  fun formatSwimDuration_showsHoursAndMinutes_fromAnHour() {
    assertEquals("1h 12min", formatSwimDuration(Duration.ofMinutes(72)))
    assertEquals("2h 0min", formatSwimDuration(Duration.ofHours(2)))
  }
}
