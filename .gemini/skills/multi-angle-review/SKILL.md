---
name: multi-angle-review
description: Performs a comprehensive 5-angle code review (Readability, Performance, Security, Test, Architecture) using specialized sub-agents and generates a quality score based on QUALITY_SCORE.md.
---

# Multi Angle Review

## Overview

This skill orchestrates five specialized sub-agents to perform a rigorous review of code changes. Each agent focuses on a specific dimension, returning structured findings and a score (0-100). The final results are weighted and synthesized into a single quality report with a pass/fail decision.

## Workflow

### 1. Preparation
Identify the review target (typically a `git diff` of the current changes) and gather context.
- **Review Target**: Changes to be reviewed.
- **Context**: Task purpose, executed tests, and TDD steps.

### 2. Build Shared Review Brief
Prepare a common context for all reviewers to ensure consistency and token efficiency.
- **Review Target**: `git diff` output.
- **Scope Summary**: 3-line summary of the task.
- **Executed Tests**: `passed/failed` summary.
- **TDD Trace**: `Red -> Green -> Refactor` summary.

### 3. Summon Sub-Agents
Invoke the following 5 agents in parallel using the `generalist` sub-agent. Each call should use the corresponding prompt from `.gemini/skills/multi-angle-review/agents/*.md`.

| Agent | Angle | Guidance Documents |
| :--- | :--- | :--- |
| **Readability** | Clarity, naming, complexity | `AGENTS.md`, `BACKEND.md`/`FRONTEND.md` |
| **Performance** | Efficiency, I/O, algorithmic | `BACKEND.md`, `ARCHITECTURE.md` |
| **Security** | Vulnerabilities, secrets, boundaries | `SECURITY.md`, `AGENTS.md` |
| **Test** | Coverage, TDD, regression | `QUALITY_SCORE.md`, `CI_WORKFLOW.md` |
| **Architecture** | Layering, modules, dependency | `ARCHITECTURE.md`, `AGENTS.md` |

**Summoning Prompt Template:**
```text
[Load Agent Prompt from agents/<agent>.md]

[Shared Review Brief]
- Target: <target>
- Summary: <context>
- Tests: <test_summary>
- TDD: <tdd_summary>
```

### 4. Aggregate & Score
Collect responses from all 5 agents. Each response must follow the structure:
- **Findings**: Severity, Component, Description.
- **Evidence**: File:Line, Code Snippet.
- **Score**: 0-100.

Calculate the **Final Score** using the weights from `QUALITY_SCORE.md`:
- **Readability**: 15%
- **Performance**: 20%
- **Security**: 25%
- **Test**: 20%
- **Architecture**: 20%

**Apply Severity Penalties:**
- **Critical**: -30
- **High**: -20
- **Medium**: -10
- **Low**: -3

### 5. Final Synthesis
Generate the report following the `Required Review Output` in `QUALITY_SCORE.md`.

## Quality Gates (from QUALITY_SCORE.md)

- **Pass**: Final Score ≥ 85 AND No Hard Gate Violations.
- **Hard Gates**: No unresolved **Critical** issues, no unresolved **High** (Security/Arch) issues, no missing/failed core tests.

## Resources

### agents/
- [readability-reviewer.md](./agents/readability-reviewer.md)
- [performance-reviewer.md](./agents/performance-reviewer.md)
- [security-reviewer.md](./agents/security-reviewer.md)
- [test-reviewer.md](./agents/test-reviewer.md)
- [architecture-reviewer.md](./agents/architecture-reviewer.md)
