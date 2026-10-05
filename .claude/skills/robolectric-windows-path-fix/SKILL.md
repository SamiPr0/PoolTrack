---
name: robolectric-windows-path-fix
description: >-
  Use when a Robolectric or Compose UI unit test fails on Windows with "Unable to load
  Robolectric native runtime library" or a NoSuchFileException on a path containing "%20"
  (e.g. C:\Users\User%20LENOVO\.m2\...android-all-instrumented-...jar). Not needed on
  macOS, Linux or CI.
---

# Robolectric fails because the Windows username contains a space

## Why it happens

Robolectric reads its `android-all-instrumented` jar from `~/.m2`. When the home directory has a
space (`C:\Users\User LENOVO`), the path is URL-encoded to `User%20LENOVO` and the file is not
found. It surfaces as one failing test per JVM run, always the first Robolectric test, so a
different test fails on each run. The test itself is fine. Linux CI is not affected.

## Fix (local only, never commit it)

Copy the jar to a folder without spaces, then point Robolectric at it. Add the two
`-Drobolectric.*` options to the same `JAVA_TOOL_OPTIONS` that `gradle-loopback-fix` sets:

```bash
mkdir -p /c/jtmp/robo
cp -n "$HOME"/.m2/repository/org/robolectric/android-all-instrumented/*/*.jar /c/jtmp/robo/
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:/jtmp -Drobolectric.offline=true -Drobolectric.dependency.dir=C:/jtmp/robo"
./gradlew testDebugUnitTest
```

Rules:
- Set the variable in every Bash call that runs Gradle; shell state does not persist.
- The folder must exist and have no spaces in its path.
- If the jar is missing from `~/.m2`, run the Robolectric tests once; the failing run still
  downloads it.
- Do not set `-Duser.home`, and do not put this in `build.gradle.kts` or CI: it is specific to
  this machine.

## Coverage

JaCoCo only records classes loaded by Robolectric's classloader because `app/build.gradle.kts`
sets `isIncludeNoLocationClasses = true` on the test task. If a Robolectric test passes but its
file shows 0% in `app/build/reports/coverage/test/debug/report.xml`, check that setting first.
