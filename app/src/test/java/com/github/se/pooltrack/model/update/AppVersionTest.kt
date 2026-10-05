package com.github.se.pooltrack.model.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

  @Test
  fun parseVersion_padsMissingPartsWithZeros() {
    assertEquals(listOf(1, 0, 0), parseVersion("1"))
    assertEquals(listOf(1, 2, 0), parseVersion("1.2"))
    assertEquals(listOf(1, 2, 3), parseVersion("1.2.3"))
  }

  @Test
  fun parseVersion_ignoresLeadingVAndWhitespace() {
    assertEquals(listOf(1, 2, 3), parseVersion(" v1.2.3 "))
  }

  @Test
  fun parseVersion_returnsNull_forMalformedVersions() {
    assertNull(parseVersion(""))
    assertNull(parseVersion("latest"))
    assertNull(parseVersion("1.2.3-beta"))
    assertNull(parseVersion("1.2.3.4"))
    assertNull(parseVersion("1..3"))
    assertNull(parseVersion("1.-2.3"))
  }

  @Test
  fun isNewerVersion_comparesEachPartNumerically() {
    assertTrue(isNewerVersion("1.0.1", "1.0.0"))
    assertTrue(isNewerVersion("1.1.0", "1.0.9"))
    assertTrue(isNewerVersion("2.0.0", "1.99.99"))
    // 10 is greater than 9: the comparison is numeric, not alphabetical.
    assertTrue(isNewerVersion("1.10.0", "1.9.0"))
  }

  @Test
  fun isNewerVersion_isFalse_forEqualVersions() {
    assertFalse(isNewerVersion("1.1.0", "1.1.0"))
    assertFalse(isNewerVersion("v1.1", "1.1.0"))
  }

  @Test
  fun isNewerVersion_isFalse_forOlderVersions() {
    assertFalse(isNewerVersion("1.0.9", "1.1.0"))
    assertFalse(isNewerVersion("0.9.0", "1.0.0"))
  }

  @Test
  fun isNewerVersion_isFalse_whenEitherVersionIsMalformed() {
    assertFalse(isNewerVersion("nightly", "1.0.0"))
    assertFalse(isNewerVersion("2.0.0", "dev"))
  }
}
