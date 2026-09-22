---
name: android-build-notes
description: Build and verification constraints specific to this Android/Compose project - Gradle cannot run in the agent sandbox, the icon library is more limited than it looks, and two model-serialization gotchas. Read before writing Compose UI code or claiming a change "builds."
---

# Android build notes for PoolTrack

## You cannot run Gradle here

Running the Gradle wrapper (build, test, lint) in this sandbox fails with
`java.io.IOException: Unable to establish loopback connection` - confirmed
via both Bash and PowerShell, with and without a sandbox-disable flag. This
is a permanent environment limitation, not something to keep retrying.

Consequence: **you cannot self-verify that a change compiles.** The user
builds in Android Studio and reports errors back (often as a screenshot of
the Build Output panel). Treat every change as unverified until they
confirm it, and when they report an error, fix the actual root cause named
in the error message rather than guessing.

Because of this, be extra careful with anything a compiler would normally
catch immediately: icon names, import paths, lambda receiver types,
argument order after changing a function signature. The checks below exist
because each one caused a real build failure in this project.

## The icon library is smaller than it looks

`app/build.gradle.kts` depends only on `androidx.compose.material:material-icons-core`,
**not** `material-icons-extended`. Most icon names you'd expect from Material
Design (e.g. `CheckBoxOutlineBlank`, `AttachMoney`, `Payments`, `Receipt`,
`ShoppingCart` is actually fine - see below) are extended-only and will fail
with "Unresolved reference" even though they're valid Material icon names in
general.

Before using any `Icons.Filled.X` / `Icons.Outlined.X` you haven't already
seen compile in this codebase:

```bash
grep -rn "androidx.compose.material.icons\." app/src/main/java/ | sort -u
```

If it's not already imported somewhere, don't guess - verify against the
actual dependency jar instead of assuming from the icon's name:

```bash
find ~ -iname "material-icons-core-*-runtime.jar" 2>/dev/null | head -1
# then, from that jar's directory:
unzip -l material-icons-core-*-runtime.jar | grep "outlined/" | sed -E 's/.*outlined\/([A-Za-z0-9]+)Kt\.class/\1/' | sort -u
```

Confirmed available (Outlined; Filled generally mirrors the same names) as
of this writing: AccountBox, AccountCircle, Add, AddCircle, ArrowBack,
ArrowDropDown, ArrowForward, Build, Call, Check, CheckCircle, Clear, Close,
Create, DateRange, Delete, Done, Edit, Email, ExitToApp, Face, Favorite,
FavoriteBorder, Home, Info, KeyboardArrowDown, KeyboardArrowLeft,
KeyboardArrowRight, KeyboardArrowUp, List, LocationOn, Lock, MailOutline,
Menu, MoreVert, Notifications, Person, Phone, Place, PlayArrow, Refresh,
Search, Send, Settings, Share, ShoppingCart, Star, ThumbUp, Warning.

If the icon you want genuinely isn't in that list, either pick a fitting one
from it, or flag to the user that adding `material-icons-extended` as a
dependency is an option (it's a real dependency change, not something to do
silently).

## Compose content-lambda receiver types

`Button`/`OutlinedButton`/`Row`/`Column` etc. take a `@Composable
<Scope>.() -> Unit` content lambda (e.g. `RowScope` for `Button`), not a
plain `@Composable () -> Unit`. This only bites when you extract the lambda
into a `val` to share between two composables (e.g. a shared "selected vs
unselected" button content) - type it with the right receiver:

```kotlin
val content: @Composable RowScope.() -> Unit = { Text(label) }
```

Passing it as a trailing lambda directly (`Button(...) { Text(label) }`)
never has this problem, since the compiler infers the receiver from context.

## Model serialization: no `Instant` fields

This project uses `kotlinx.serialization` (via `Json.encodeToString`/
`decodeFromString`) for `Subscription` and `Entry`, stored as JSON in
Jetpack DataStore. `kotlinx.serialization` has no built-in support for
`java.time.Instant`. The established pattern (see `Subscription.kt`,
`Entry.kt`): store the raw `Long` epoch-millis field, and expose the
`Instant` via a same-file extension property:

```kotlin
@Serializable
data class Entry(val timestampEpochMilli: Long, val subscriptionId: String? = null)

val Entry.timestamp: Instant
  get() = Instant.ofEpochMilli(timestampEpochMilli)
```

Follow this for any new model needing a timestamp. Also remember: a
same-package extension property/function (like `Entry.timestamp` above)
still needs an explicit `import` in files outside that package - importing
the class itself does not pull in its extensions.

If you add a field to an existing `@Serializable` data class, give it a
default value (`= null` or otherwise) so previously-stored JSON without that
field still decodes without extra configuration.

## Formatting

Files in this repo use ktfmt-style formatting: ~100-character line width,
2-space indentation, trailing commas in multi-line calls. Before considering
an edit done, check line lengths on the files you touched:

```bash
awk '{ if (length($0) > 100) print NR": "length($0) }' path/to/File.kt
```
