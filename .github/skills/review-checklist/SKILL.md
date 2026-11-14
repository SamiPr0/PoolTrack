---
name: review-checklist
description: How to review an agent's pull request in PoolTrack
---
Review against AGENTS.md and these points:
- the tests pass, and they actually assert behaviour, not just counts or empty checks;
- the diff is bounded and reviewable; a small feature should not touch unrelated files;
- error handling and edge cases are covered;
- the code follows MVVM and does not touch generated files;
- CI is green on the PR;
- contributors are acknowledged: each commit credits the AI with a `Co-authored-by` line.
  Report each point as OK or ISSUE, and refuse to approve what you cannot review.
