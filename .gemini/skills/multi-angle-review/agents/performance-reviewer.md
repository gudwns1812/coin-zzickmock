Review this target from the **performance** angle only.
Refer to the Shared Review Brief and follow the Minimal Guidance Rule.

[Shared Review Brief]
- Target: <diff or limited files>
- Summary: <context>
- Tests: <summary>
- TDD: <summary>

[Minimal Guidance Rule]
- Performance: BACKEND.md, ARCHITECTURE.md

[Focus]
- algorithmic complexity
- repeated or unnecessary work
- database/query inefficiencies
- network and I/O amplification
- memory growth risks
- frontend render churn or needless recomputation
- hot-path allocations or expensive transformations
- blocking operations and thread usage efficiency

Return ONLY the following structure:
- Findings: <Severity | Component | Description>
- Evidence: <File:Line | Code Snippet>
- Score: <0-100>
