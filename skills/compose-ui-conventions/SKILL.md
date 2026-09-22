---
name: compose-ui-conventions
description: UI patterns this codebase already uses for Compose screens - test tags, the colored IconBadge accent pattern, dialog styling, and locale-safe formatters. Follow these instead of inventing new ones so new screens look and test like the existing ones.
---

# Compose UI conventions for PoolTrack

These are patterns already established across `HomeScreen.kt`,
`SubscriptionScreen.kt`, `HistoryScreen.kt`, etc. Match them for new screens
rather than introducing a new one-off style.

## Test tags

Every screen file defines its own `object <Screen>TestTags` with `const val`
entries, named `"<Screen><Element>"` (e.g. `SubscriptionScreenTestTags.ADD_BUTTON
= "SubscriptionScreenAddButton"`). Every interactive or content-bearing
element gets `Modifier.testTag(...)` from that object - buttons, list items,
dialogs, empty-state messages. Add a new tag whenever you add a new element
a test might need to find; don't reuse another screen's tag object.

## Colored icon badges ("IconBadge")

Stat tiles, banners, and prompt cards use a small colored circle behind
their icon rather than a bare tinted icon on a plain background - see
`IconBadge` in `HomeScreen.kt`. When adding a new stat/banner, reuse this
shape: a `Box` clipped to `CircleShape`, background = a *solid* theme color
(`primary`/`secondary`/`tertiary`/`error`, not the `...Container` variant),
icon tinted with the matching `on...` color. Reserve the `...Container`
colors for the *card's* background when the whole card should read as
tinted (e.g. an urgent/actionable banner); keep plain stat tiles on a
neutral card with just the badge colored, so the screen doesn't turn into a
wall of colored rectangles.

Cycle `primary`/`secondary`/`tertiary` across a grid of tiles rather than
tinting every tile the same color - see the stat grid in `HomeScreen.kt` for
the pattern.

## Dialogs: `AlertDialog` vs custom `Dialog`

Use Material's `AlertDialog` for simple confirm/cancel prompts (delete
confirmation, a short form). For anything needing real visual structure
(an image/preview, multiple sections, custom spacing) use a custom
`Dialog(onDismissRequest = ...) { Surface(...) { Column(...) { ... } } }`
instead of fighting `AlertDialog`'s fixed slots - see
`SubscriptionDetailDialog` in `SubscriptionScreen.kt`.

When a `Button`/`OutlinedButton`'s content lambda is shared between two call
sites (e.g. a toggle-style "selected vs not" button), type it explicitly as
`@Composable RowScope.() -> Unit` - see `android-build-notes` for why.

## Locale-safe formatters

Every `DateTimeFormatter` (or similar) is built inside a private function
called fresh on each use, never cached in a top-level `val`:

```kotlin
private fun addedDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
```

This is deliberate: a top-level `val` bakes in `Locale.getDefault()` at
class-init time, so a locale change while the app is running wouldn't be
picked up. Follow the same pattern for any new formatter.

## Rounding durations for display

Don't show a raw day count once it gets large ("expires in 363 days") -
round to the largest sensible unit using `java.time.Period.between(...)`
(years, then months, then days), with "today"/"tomorrow" special-cased at
the day level. See `expirationMessage`/`humanizePeriod` in `HomeScreen.kt`.
