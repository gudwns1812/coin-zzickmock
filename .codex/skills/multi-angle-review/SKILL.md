---
name: multi-angle-review
description: Run independent multi-angle code reviews by spawning fresh project-scoped reviewer subagents for readability, performance, security, test quality, and architecture, then merge the results into one report. Use when Codex needs a repeatable review panel for a diff, working tree, pull request, or file set without letting prior evaluations contaminate later passes.
---

# Multi Angle Review

## Overview

Use this skill to review the same code change from multiple independent quality angles.
Each angle runs in a fresh subagent so one reviewer's conclusions do not bias the others.

## Workflow

1. Identify the review target.
   Prefer an explicit diff, working tree, commit range, pull request patch, or file list from the user.
   If the user did not narrow the scope, review the current working tree changes.
2. Spawn one fresh subagent per angle using the project-scoped custom agents in `.codex/agents/`:
   - `readability-reviewer`
   - `performance-reviewer`
   - `security-reviewer`
   - `test-reviewer`
   - `architecture-reviewer`
3. Keep every pass isolated.
   - Do not pass findings from one reviewer into another reviewer.
   - Give each reviewer only the target, the angle, and any raw artifacts needed to inspect the current snapshot.
   - Prefer fresh subagents/threads for every pass.
   - Avoid including prior conclusions, intended fixes, or expected answers in reviewer prompts.
4. Wait for the reviewers to finish, then merge the results into one synthesized report.
5. Preserve angle-specific findings even when they overlap. Deduplicate only near-identical duplicates after the merge step.

## Reviewer Prompt Shape

Use a short prompt for each reviewer that looks like a real user request. Keep it angle-local.

Example structure:

```text
Review the current target using the `security-reviewer` custom agent.
Inspect only the current snapshot and do not rely on prior reviews.
Return:
- findings ordered by severity
- concrete evidence with file references
- residual risks if no issues are found
```

## Synthesis Rules

- Keep findings grouped by angle first.
- Within each angle, order findings by severity and then by confidence.
- Merge only truly duplicate findings. When the same issue appears in multiple angles, keep one main entry and note the other angles that also flagged it.
- If one reviewer reports no findings, include that explicitly instead of omitting the angle.
- Separate "findings" from "open questions" and "residual risks."
- Do not weaken a severe finding just because other reviewers did not mention it.

## Output Format

Use this shape unless the user asks for a different one:

### Summary

- One short paragraph on overall risk and review scope.

### Findings

- Flat list of merged findings with:
  - severity
  - angle
  - concise explanation
  - file/path evidence

### Angle Notes

- `Readability`: short recap or "no findings"
- `Performance`: short recap or "no findings"
- `Security`: short recap or "no findings"
- `Test`: short recap or "no findings"
- `Architecture`: short recap or "no findings"

### Open Questions

- Only include unresolved questions that materially affect confidence.

## Project Notes

- This repo contains both `backend/` and `frontend/`.
- When reviewing backend changes, honor the rules in `backend/AGENTS.md`, especially package boundaries, thin services, response/exception consistency, and regression-test expectations.
- When reviewing frontend changes, prefer findings tied to real behavior, maintainability, and rendering/runtime impact over personal style preferences.
