Review this target from the **readability** angle only.
Refer to the Shared Review Brief and follow the Minimal Guidance Rule.

[Shared Review Brief]
- Target: <diff or limited files>
- Summary: <context>
- Tests: <summary>
- TDD: <summary>

[Minimal Guidance Rule]
- Readability: AGENTS.md, BACKEND.md (또는 FRONTEND.md)

[Focus]
- naming quality
- local complexity and mental overhead
- duplication that hurts comprehension
- unclear control flow
- hidden coupling that makes the code harder to follow
- comments or documentation gaps that materially increase confusion

Return ONLY the following structure:
- Findings: <Severity | Component | Description>
- Evidence: <File:Line | Code Snippet>
- Score: <0-100>
