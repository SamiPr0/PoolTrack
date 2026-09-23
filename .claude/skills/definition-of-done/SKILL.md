---
name: definition-of-done
description: Use before declaring any code change finished, committing, or opening a PR in this repo. Runs the checks from AGENTS.md in order and says how to commit and report.
---

The rules live in AGENTS.md ("Definition of done", "How to work"). This is the procedure.

## 1. Check

Run these in order and stop at the first failure. Fix the failure and start again from the top.

1. `./gradlew ktfmtFormat`
2. `./gradlew ktfmtCheck`
3. `./gradlew testDebugUnitTest --tests "*<ClassUnderTest>Test*"` (fast feedback)
4. `./gradlew check`
5. Only if the change touches code covered by `androidTest/`:
   `./gradlew connectedDebugAndroidTest` with an emulator running.
   If none is available, say so. Do not skip silently.

## 2. Self-review the diff

- `git diff` touches only files related to the task, and no generated code.
- New code has unit tests.
- ViewModels import no Firebase, no repository implementation and no `Application`.

## 3. Commit

- Stage files by explicit path. Never use `git add .` or `git add -A`.
- Imperative, capitalized subject of at most 50 characters. Add a body wrapped at 72
  characters if the subject is not enough.
- Credit the AI with a `Co-authored-by` line.
- The ktfmt pre-commit hook runs `ktfmtCheck`. Never skip it with `--no-verify`.

## 4. Report

List the files changed, which acceptance criteria each test covers, the
Gradle result line of each command above, and anything skipped or not verified.
Once pushed, CI (`.github/workflows/CI.yml`) must also be green on the PR.
