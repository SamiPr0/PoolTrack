---
name: pre-pr-review
description: Which of Claude Code's built-in review skills to run before opening or updating a PR in this repo, and why - since Gradle can't run here (see android-build-notes), these reviews are the only automated check a change gets before the user builds it in Android Studio.
---

# Pre-PR review for PoolTrack

This repo can't compile in the agent sandbox (see `android-build-notes`), so
there is no CI-in-the-loop feedback before the user builds a change
themselves. Claude Code's built-in review skills are the only automated
check that happens before that - use them instead of skipping straight to
"commit and hope."

These are global skills, not files in this repo - invoke them with the
`Skill` tool (`/code-review`, `/simplify`, `/security-review`) rather than
looking for their content here. This file only records *when* to reach for
each one on this project specifically.

## `code-review`

Run before opening a PR for any non-trivial change (more than a
one-or-two-line fix). It catches correctness bugs a compiler can't -
exactly the category of mistake that's expensive here, since a wrong
`.timestamp` extension import or a swapped argument order won't surface
until the user builds it. Low/medium effort is enough for routine feature
work; reach for high effort on anything touching money (`price`,
`pricePerEntry`, `totalSpent`) or the cooldown/expiration logic, where a
subtle off-by-one is easy to miss and easy to ship.

## `simplify`

Run after a feature is functionally done but before the final commit, on
any change that touched more than one existing file. This codebase leans on
small, repeated composables (`StatTile`, `IconBadge`, `ChoiceButton`) - it's
easy to duplicate one of these patterns instead of reusing it, and
`simplify` is the right tool to catch that before the PR grows another
"pretty much the same as StatTile" case.

## `security-review`

Not routine for this project's current feature set (no auth, no network
calls, everything stored locally in DataStore) - skip it for ordinary
subscription/entry/UI work. It becomes relevant the moment a change touches
Firebase Auth, Firestore, or any other backend/network surface - which is
exactly what the (currently unmerged) auth/backup work on
`feat/subscription-tab-is-management` does. If you pick that work back up,
run `security-review` before opening its PR: credential handling, what gets
written to Firestore under which user, and whether local data can leak
across accounts are all real risks that a compiler and `code-review` alone
won't catch.

## What none of these replace

They review the diff; they don't run the app. Nothing here substitutes for
the user actually building in Android Studio and exercising the golden
path - see `android-build-notes` for why that step can't be automated away
in this environment.
