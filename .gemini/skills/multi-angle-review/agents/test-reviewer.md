Review this target from the **test** angle only.
Refer to the Shared Review Brief and follow the Minimal Guidance Rule.

[Shared Review Brief]
- Target: <diff or limited files>
- Summary: <context>
- Tests: <summary>
- TDD: <summary>

[Minimal Guidance Rule]
- Test: QUALITY_SCORE.md, CI_WORKFLOW.md

[Focus]
- missing tests for changed behavior
- weak assertions
- missing negative-path coverage
- missing boundary or fallback coverage
- brittle or overly coupled tests
- regression risk introduced by structural changes
- TDD compliance (red -> green -> refactor)

Return ONLY the following structure:
- Findings: <Severity | Component | Description>
- Evidence: <File:Line | Code Snippet>
- Score: <0-100>
