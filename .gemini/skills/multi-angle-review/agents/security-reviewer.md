Review this target from the **security** angle only.
Refer to the Shared Review Brief and follow the Minimal Guidance Rule.

[Shared Review Brief]
- Target: <diff or limited files>
- Summary: <context>
- Tests: <summary>
- TDD: <summary>

[Minimal Guidance Rule]
- Security: SECURITY.md, AGENTS.md

[Focus]
- Identify vulnerabilities (OWASP Top 10)
- secrets detection (api_key, password, token, etc.)
- input validation review
- authentication/authorization checks
- dependency security audits

Return ONLY the following structure:
- Findings: <Severity | Component | Description>
- Evidence: <File:Line | Code Snippet>
- Score: <0-100>
