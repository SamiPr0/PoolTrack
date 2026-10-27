---
name: merge-when-green
description: Use after opening a PR in this repo, and whenever the user asks to merge a PR. Watches CI and SonarCloud, fixes failures, asks before changing behaviour, and merges only if the user asked.
---

`main` is protected: Always-on checks, Firestore rules tests, Tests and SonarCloud Code Analysis
must pass, and there is no admin bypass. Never try to get around that (`gh pr merge --admin`,
editing the protection, weakening the Quality Gate or the JaCoCo thresholds, adding exclusions or
`@Suppress` just to turn a check green).

## 1. Keep an eye on every PR you open

- Right after `gh pr create`, call `get_status` and, if it does not report the PR, `bind_pr`.
- Turn on `set_monitor` with auto-fix, so CI failures and review comments wake the session.
- Do not poll CI yourself (no `sleep` loops, `gh pr checks --watch`, `/loop`, cron). The monitor
  reports it.

## 2. When the user asks to merge

"Merge" means: only once CI **and** SonarCloud are green **and** a reviewer approved. It is the
user's explicit go-ahead for that PR only, not for others.

1. `get_status` and read the checks. Pending: wait for the monitor event, then look again.
2. Reviewer: a session that did not write the PR must have approved its current head. None yet, or
   the head changed since the approval: start a reviewer session (read-only, `review-checklist`)
   and wait for its verdict. After every round of requested changes, a brand-new reviewer session
   reviews again, never the previous one, and without the earlier findings in its brief.
3. All four required checks pass and a reviewer approved: merge with a merge commit
   (`gh pr merge <n> --merge`), then report what was merged. Never merge a PR the user did not
   ask to merge.
4. Something failed: go to section 3, then wait for the new run. Repeat until green.

If the user did not ask to merge, stop at green and say so. Do not merge. If no reviewer approved,
do not merge even when the user asked: start the review and say so.

## 3. A check failed

Find the cause, do not guess:

- GitHub Actions: `gh run view <run-id> --log-failed`.
- SonarCloud (public API, no token): the gate is
  `https://sonarcloud.io/api/qualitygates/project_status?projectKey=SamiPr0_PoolTrack&pullRequest=<n>`
  and the issues are
  `https://sonarcloud.io/api/issues/search?componentKeys=SamiPr0_PoolTrack&pullRequest=<n>&resolved=false`.
  Coverage on new code counts lines and branches: add the missing tests.
- Local Windows runs need the `gradle-loopback-fix` and `robolectric-windows-path-fix` skills.

Then decide:

- **Fix without changing behaviour** (formatting, a flaky or missing test, a lint or Sonar smell,
  coverage, a typo, a refactor with the same behaviour): fix it, run `definition-of-done`, commit
  (one commit per fix, explicit paths), push, and wait for the new run. Do not ask first.
- **The fix would change behaviour** (what the app does for the user, a data format, a permission,
  a test expectation that encodes behaviour, dropping a feature to satisfy a rule): stop and ask the
  user, with the options and your recommendation. Do not push it unasked.
- **Not caused by the PR** (outage, infra, a check broken on `main`): say so; do not hack around it.

## 4. Report

State what failed, what you changed, the final check results, and whether the PR was merged. If
you did not verify something (for example on a device), say so.
