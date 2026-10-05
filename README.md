# PoolTrack

Tracks my pool entries.

## Features

- Display your subscription (a PDF) at maximum screen brightness, so the scanner at the pool entrance can read it.
- Keep a log of the dates the subscription was scanned.
- Only count a scan as an entry once you confirm the scanner accepted it. A declined scan is not counted.
- Sign in with Google to back up your data.

## Screenshots

Coming soon.

## Install

1. Open the [latest release](https://github.com/SamiPr0/PoolTrack/releases/latest) on your phone and download the `.apk` file.
2. Open it. Android asks you to allow installs from your browser or file manager; this is needed once.
3. Requires Android 10 (API 29) or newer.

Once installed, PoolTrack checks for new releases when it starts and offers to update itself.

## User stories

- As a user, I should be able to display my subscription (a PDF) on the phone screen when getting to the scanners with max brightness so that I can enter the pool.
- As a user, I should be able to see the dates when I scanned the subscription so that I can keep track of all my entries.
- As a user, I should be able to confirm that the scanner worked so it can count as an entry. Similarly, if it's declined, it shouldn't be counted as an entry.

## Development

PoolTrack is a Kotlin app built with Jetpack Compose and an MVVM architecture (`model/` for data and repositories, `ui/` for screens and ViewModels).

To build it yourself, create a Firebase project, register the Android app `com.github.se.pooltrack`, and save its `google-services.json` as `app/google-services.json`. The file is git-ignored.

```bash
./gradlew assembleDebug     # build
./gradlew check             # unit tests, lint, coverage gate
./gradlew ktfmtCheck        # formatting
```

Contributions follow the rules in [AGENTS.md](AGENTS.md).
