# AGENTS.md

Durable rules for any AI agent working in this repository. Read this before acting.

## The app

PoolTrack — a Kotlin/Android app (`com.github.se.pooltrack`) built with Jetpack Compose. It tracks pool entries:

- Displays the user's subscription (a PDF) at max screen brightness so it can be scanned at the pool entrance.
- Keeps a log of the dates the subscription was scanned.
- Only counts a scan as an entry once the user confirms the scanner accepted it; a declined scan is not counted.

## How to work

- **Never commit directly to `main`.** Work on a feature branch and open a pull request; the human reviews and merges it.
- **Commit after every change**, however small. Don't batch unrelated changes into one commit.
- Stage only the files you changed; never `git add .` or `git add -A` (it can pull in local config like `local.properties`, `.idea/`, `.gradle/`).
- Commit with an imperative subject of at most 50 characters, capitalized (e.g. `Add subscription PDF viewer`). Add a body wrapped at 72 characters when the subject is not enough.
- Acknowledge contributors: credit the AI that wrote the change with a `Co-authored-by` line.
- Push the branch and open a PR against `main` once the change is ready for review. Don't merge it yourself.

## Your role

You provide the goal, the context, the acceptance criteria and the permissions. The agent plans, acts and observes. **You review the diff, and you own every line you submit.** "The agent wrote it" is not a defence.
