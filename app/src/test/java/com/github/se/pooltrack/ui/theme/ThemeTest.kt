package com.github.se.pooltrack.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun resolveScheme(darkTheme: Boolean?): ColorScheme {
    lateinit var scheme: ColorScheme
    composeRule.setContent {
      if (darkTheme == null) {
        PoolTrackTheme { scheme = MaterialTheme.colorScheme }
      } else {
        PoolTrackTheme(darkTheme = darkTheme) { scheme = MaterialTheme.colorScheme }
      }
    }
    composeRule.waitForIdle()
    return scheme
  }

  private fun lightAppearanceStatusBars(): Boolean {
    val window = composeRule.activity.window
    return WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
  }

  private fun assertLight(scheme: ColorScheme) {
    assertEquals(Color(0xFF00668A), scheme.primary)
    assertEquals(Color(0xFFFFFFFF), scheme.onPrimary)
    assertEquals(Color(0xFFC3E7FF), scheme.primaryContainer)
    assertEquals(Color(0xFF001E2C), scheme.onPrimaryContainer)
    assertEquals(Color(0xFF4E616D), scheme.secondary)
    assertEquals(Color(0xFFFFFFFF), scheme.onSecondary)
    assertEquals(Color(0xFFD1E5F4), scheme.secondaryContainer)
    assertEquals(Color(0xFF0A1E28), scheme.onSecondaryContainer)
    assertEquals(Color(0xFF00658F), scheme.tertiary)
    assertEquals(Color(0xFFFFFFFF), scheme.onTertiary)
    assertEquals(Color(0xFFC7E7FF), scheme.tertiaryContainer)
    assertEquals(Color(0xFF001E2E), scheme.onTertiaryContainer)
    assertEquals(Color(0xFFBA1A1A), scheme.error)
    assertEquals(Color(0xFFFFFFFF), scheme.onError)
    assertEquals(Color(0xFFFFDAD6), scheme.errorContainer)
    assertEquals(Color(0xFF410002), scheme.onErrorContainer)
    assertEquals(Color(0xFF71787D), scheme.outline)
    assertEquals(Color(0xFFFBFCFF), scheme.background)
    assertEquals(Color(0xFF191C1E), scheme.onBackground)
    assertEquals(Color(0xFFF8F9FC), scheme.surface)
    assertEquals(Color(0xFF191C1E), scheme.onSurface)
    assertEquals(Color(0xFFDCE3E9), scheme.surfaceVariant)
    assertEquals(Color(0xFF41484D), scheme.onSurfaceVariant)
    assertEquals(Color(0xFF2E3133), scheme.inverseSurface)
    assertEquals(Color(0xFFF0F1F3), scheme.inverseOnSurface)
    assertEquals(Color(0xFF7BD0FF), scheme.inversePrimary)
    assertEquals(Color(0xFF00668A), scheme.surfaceTint)
    assertEquals(Color(0xFFC0C7CD), scheme.outlineVariant)
    assertEquals(Color(0xFF000000), scheme.scrim)
  }

  private fun assertDark(scheme: ColorScheme) {
    assertEquals(Color(0xFF7BD0FF), scheme.primary)
    assertEquals(Color(0xFF003549), scheme.onPrimary)
    assertEquals(Color(0xFF004C69), scheme.primaryContainer)
    assertEquals(Color(0xFFC3E7FF), scheme.onPrimaryContainer)
    assertEquals(Color(0xFFB5C9D7), scheme.secondary)
    assertEquals(Color(0xFF20333E), scheme.onSecondary)
    assertEquals(Color(0xFF364955), scheme.secondaryContainer)
    assertEquals(Color(0xFFD1E5F4), scheme.onSecondaryContainer)
    assertEquals(Color(0xFF86CFFF), scheme.tertiary)
    assertEquals(Color(0xFF00344C), scheme.onTertiary)
    assertEquals(Color(0xFF004C6D), scheme.tertiaryContainer)
    assertEquals(Color(0xFFC7E7FF), scheme.onTertiaryContainer)
    assertEquals(Color(0xFFFFB4AB), scheme.error)
    assertEquals(Color(0xFF690005), scheme.onError)
    assertEquals(Color(0xFF93000A), scheme.errorContainer)
    assertEquals(Color(0xFFFFDAD6), scheme.onErrorContainer)
    assertEquals(Color(0xFF8B9297), scheme.outline)
    assertEquals(Color(0xFF191C1E), scheme.background)
    assertEquals(Color(0xFFE1E2E5), scheme.onBackground)
    assertEquals(Color(0xFF111416), scheme.surface)
    assertEquals(Color(0xFFC5C6C9), scheme.onSurface)
    assertEquals(Color(0xFF41484D), scheme.surfaceVariant)
    assertEquals(Color(0xFFC0C7CD), scheme.onSurfaceVariant)
    assertEquals(Color(0xFFE1E2E5), scheme.inverseSurface)
    assertEquals(Color(0xFF191C1E), scheme.inverseOnSurface)
    assertEquals(Color(0xFF00668A), scheme.inversePrimary)
    assertEquals(Color(0xFF7BD0FF), scheme.surfaceTint)
    assertEquals(Color(0xFF41484D), scheme.outlineVariant)
    assertEquals(Color(0xFF000000), scheme.scrim)
  }

  @Test
  fun poolTrackTheme_usesTheLightPalette_whenNotDark() {
    assertLight(resolveScheme(darkTheme = false))
  }

  @Test
  fun poolTrackTheme_usesTheDarkPalette_whenDark() {
    assertDark(resolveScheme(darkTheme = true))
  }

  @Test
  @Config(qualifiers = "notnight")
  fun poolTrackTheme_followsTheSystem_lightByDefault() {
    assertLight(resolveScheme(darkTheme = null))
  }

  @Test
  @Config(qualifiers = "night")
  fun poolTrackTheme_followsTheSystem_darkByDefault() {
    assertDark(resolveScheme(darkTheme = null))
  }

  @Test
  fun poolTrackTheme_usesDarkStatusBarIcons_inLightTheme() {
    resolveScheme(darkTheme = false)

    assertTrue(lightAppearanceStatusBars())
  }

  @Test
  fun poolTrackTheme_usesLightStatusBarIcons_inDarkTheme() {
    resolveScheme(darkTheme = true)

    assertFalse(lightAppearanceStatusBars())
  }

  @Test
  fun poolTrackTheme_usesMaterialDefaultTypography() {
    lateinit var typography: MaterialTypography
    composeRule.setContent {
      PoolTrackTheme(darkTheme = false) { typography = MaterialTheme.typography }
    }
    composeRule.waitForIdle()

    assertEquals(MaterialTypography().titleLarge, typography.titleLarge)
  }

  @Test
  fun seed_isTheBrandBlue() {
    assertEquals(Color(0xFF008BBB), seed)
  }

  @Test
  fun typography_definesTheBodyLargeStyle() {
    val style = Typography.bodyLarge

    assertEquals(FontFamily.Default, style.fontFamily)
    assertEquals(FontWeight.Normal, style.fontWeight)
    assertEquals(16.sp, style.fontSize)
    assertEquals(24.sp, style.lineHeight)
    assertEquals(0.5.sp, style.letterSpacing)
  }

  @Test
  fun typography_keepsMaterialDefaults_forOtherStyles() {
    assertEquals(MaterialTypography().bodyMedium, Typography.bodyMedium)
    assertEquals(MaterialTypography().headlineSmall, Typography.headlineSmall)
  }
}
