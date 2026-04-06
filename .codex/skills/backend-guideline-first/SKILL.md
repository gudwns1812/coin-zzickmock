---
name: backend-guideline-first
description: Read and apply the stock-zzickmock backend guideline before changing backend code. Use when working on Java or Spring backend files under /Users/hj.park/projects/stock-zzickmock/backend for new features, refactors, API changes, exception or response changes, package restructuring, storage changes, external integration work, batch work, or backend test updates.
---

# Backend Guideline First

Open `/Users/hj.park/projects/stock-zzickmock/backend/docs/BACKEND_GUIDELINE.md` before any backend implementation in this repository.
Do not start coding, refactoring, or reviewing backend structure until you have read the guideline and identified the rules that apply to the task.

## Required Workflow

1. Read `/Users/hj.park/projects/stock-zzickmock/backend/docs/BACKEND_GUIDELINE.md` first.
2. Extract the rules that affect the current task before editing code.
3. Use those rules to decide package placement, service responsibility boundaries, exception handling, response shape, and test scope.
4. Keep implementation consistent with the guideline even if older code in the repository violates it.
5. Treat any inconsistency you find as a refactoring target when it is in scope for the task.
6. Verify the result with backend regression tests, typically `./gradlew clean test` in `/Users/hj.park/projects/stock-zzickmock/backend`.

## What To Check In The Guideline

Always check the parts that match the task:

- Package placement across `core`, `storage`, `support`, `extern`, `batch`, and any newly needed top-level responsibility
- Controller rules, especially `ApiResponse` and `HttpStatus` usage
- Exception rules, especially `CoreException` and `ErrorType`
- Service rules, especially keeping orchestration in service and moving details into collaborators
- DTO rules, especially static factory methods such as `from(...)` and `of(...)`
- External API rules for `extern`
- Scheduler rules for `batch`
- Test and regression expectations

## Implementation Rules

Prefer the guideline over local convenience.
When the current code and the guideline conflict, align new or changed code with the guideline unless the user explicitly asks to preserve the old pattern.

Keep these defaults:

- Keep backend business behavior unchanged unless the user explicitly asks for a behavior change.
- Refactor for consistency when touching nearby code that clearly violates the guideline.
- Keep services thin and move detailed logic into appropriately named collaborators.
- Keep response and exception handling unified.
- Keep external integrations out of `support`.
- Keep scheduled jobs in `batch`.

## Failure Handling

If `/Users/hj.park/projects/stock-zzickmock/backend/docs/BACKEND_GUIDELINE.md` is missing, unreadable, or clearly outdated for the task, pause and report that the repository guideline could not be used as intended.
Do not silently skip the guideline step.
