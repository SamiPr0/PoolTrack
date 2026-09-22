---
name: git-workflow
description: PoolTrack's branch/PR/merge workflow - when to branch, how to commit, and the stacked-PR gotcha where commits pushed after a PR merges silently don't reach main. Read this before making any commit or opening/merging a PR in this repo.
---

# Git workflow for PoolTrack

The durable rules (never commit to `main`, commit per change, attribution
trailers, don't merge without being asked) live in [AGENTS.md](../../../AGENTS.md)
at the repo root - read that first. This skill covers what AGENTS.md doesn't:
staging discipline and one recurring, easy-to-miss failure mode.

## Staging discipline

AGENTS.md says: stage only the files you changed, never `git add .` or
`git add -A`. In practice, run `git status` before staging, add each changed
path by name, and re-run `git status` after staging to confirm nothing
unexpected (e.g. `local.properties`, `.idea/`, stray build output) got pulled
in.

## The stacked-PR gotcha

**This has happened at least three times in this project.** The failure
mode: you push commit A, open a PR, then keep pushing commits B, C, D to the
same branch while the PR is still open. The user merges the PR on GitHub -
but if they click merge while the PR's snapshot was still at commit A (or
GitHub simply merges whatever the PR page shows at that moment), commits B,
C, D are **not** included in the merge, even though they're sitting right
there on the same branch. `git log` on that branch will happily show them
as "part of the branch," which makes it easy to assume they already made it
into `main`. They didn't.

A related variant: a *different* branch's PR gets merged first, and it
touches files your branch also touched - `main` moves out from under you,
and you find out only when a later merge or pull surfaces a conflict.

### How to check whether this happened

Whenever the user says something like "merged," "pull this into main," "I
merged them," or just "merge to main":

```bash
git fetch origin
git log --oneline origin/main -5
git log --oneline origin/main..origin/<your-feature-branch>
git merge-base origin/main origin/<your-feature-branch>
```

If the second command lists any commits, those are on your branch but not
in `main` - even if a PR for that branch shows as "Merged" on GitHub. Open a
**new** PR from the same branch to `main` to carry them over (same branch,
new PR - GitHub allows this once the previous PR for that branch is closed
or merged).

### How to avoid triggering it yourself

- After opening a PR, prefer to pause and let the user review/merge before
  pushing more unrelated commits to the same branch. If you must keep
  pushing (e.g. the user is actively iterating with you), that's fine - just
  re-run the check above after any merge, don't assume it happened cleanly.
- Before starting new work "on the feature branch," check `git log
  origin/main..<branch>` first. If the branch already has commits ahead of
  `main` that predate your new task, find out whether they're intentional
  in-progress work (someone else's, or an earlier session's) before you add
  to them or branch off them. If they're unrelated to what you're about to
  do, branch off `main` instead, not off that branch.

## Merging

Per AGENTS.md, you open PRs but don't merge them yourself unless the user
explicitly asks (e.g. "merge to main," "merge it"). A generic "commit this"
or "push" is not that ask.
