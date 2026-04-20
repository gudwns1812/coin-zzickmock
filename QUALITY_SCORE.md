# QUALITY_SCORE.md

## Purpose

이 문서는 에이전트 루프의 검증 단계와 리뷰 단계를 위한 품질 게이트 기준서다.
목표는 실제 `multi-angle-review` 실행 결과를 점수화하여 비용 효율적이면서도 엄격한 종료 조건을 설정하는 것이다.

## When To Use

아래 상황에서 루프의 마지막 단계에 적용한다.

- 코드 수정/리팩터링 직후
- 주요 문서 기준 변경 직후
- PR 전 최종 품질 게이트 리뷰

**검토 범위 제한:**

- 사용자가 명시적으로 지정한 diff, 파일, 경로만 검토한다.
- 범위 지시가 없으면 전체 저장소를 검토하지 않고 범위를 먼저 확정한다.

## Review Panel (Multi-Angle Review)

리뷰는 반드시 독립된 5개 각도로 수행한다. 수동 self-review나 단독 점수 추정은 인정하지 않는다.

1. **readability-reviewer**: 가독성, 네이밍, 책임 분리
2. **performance-reviewer**: 자원 효율성, I/O 비용
3. **security-reviewer**: 보안 경계, 민감 정보, 입력 신뢰성
4. **test-reviewer**: 커버리지, TDD 이행, 회귀 방지
5. **architecture-reviewer**: 레이어링, 의존성 방향, 모듈 경계

## Token Efficiency Rules (Shared Review Brief)

토큰 소비를 최소화하기 위해 공통 입력은 `Shared Review Brief`로 1회만 정의하고, 각 리뷰어는 이를 참조한다.

### 1. Shared Review Brief 구성

- **Review Target**: `git diff` 위주로 구성 (파일 전체보다 변경점 우선)
- **Scope Summary**: 작업 목적 및 범위 요약 (3줄 이내)
- **Executed Tests Summary**: `passed/failed` 위주의 요약 로그 (상세 스택 트레이스 제외)
- **TDD Trace Summary**: `Red -> Green -> Refactor` 단계별 핵심 변경 요약
- **Known Constraints**: 알려진 제약 사항 요약

### 2. Minimal Guidance Rule (리뷰어별 맞춤 문서)

리뷰어에게는 해당 각도와 관련된 최소한의 문서만 전달한다.

- **Readability**: AGENTS.md, BACKEND.md (또는 FRONTEND.md)
- **Performance**: BACKEND.md, ARCHITECTURE.md
- **Security**: SECURITY.md, AGENTS.md
- **Test**: QUALITY_SCORE.md, CI_WORKFLOW.md
- **Architecture**: ARCHITECTURE.md, AGENTS.md

### 3. Response Compression

리뷰어의 응답은 서술적 문장을 배제하고 Finding, Evidence, Score 위주로 압축하여 토큰을 절약한다.

## Reviewer Prompt Contract

```text
Review this target from the <angle> angle only.
Refer to the Shared Review Brief and follow the Minimal Guidance Rule.

[Shared Review Brief]
- Target: <diff or limited files>
- Summary: <context>
- Tests: <summary>
- TDD: <summary>

Return ONLY the following structure:
- Findings: <Severity | Component | Description>
- Evidence: <File:Line | Code Snippet>
- Score: <0-100>
```

## Scoring Model

### Angle Weights

- Readability: 15 / Performance: 20 / Security: 25 / Test: 20 / Architecture: 20 (Total: 100)

### Severity Penalty

- **Critical**: -30 / **High**: -20 / **Medium**: -10 / **Low**: -3
- 중복 지적은 감점 1회만 적용하되 리스크 확정 근거로 활용한다.

## Hard Gates (Pass 불가 조건)

아래 중 하나라도 해당하면 총점과 무관하게 `Major Rework` 또는 `Fix` 판정한다.

- unresolved **Critical** finding 존재
- unresolved **High** (Security/Architecture) finding 존재
- 핵심 테스트 실패 또는 정당한 사유 없는 테스트 누락
- 동작 변경임에도 이를 검증하는 테스트/TDD 흔적 없음

## Exit Thresholds

- **Pass (종료)**: Final Score ≥ 85 AND No Hard Gate Violation
- **Fix And Re-run**: Final Score 70~84 OR unresolved Medium finding 존재
- **Major Rework**: Final Score < 70 OR Hard Gate Violation

## Required Review Output (Final Synthesis)

최종 보고에는 아래 항목을 반드시 포함한다.

1. **Review Target & Scope Summary**
2. **Multi-Angle Scores** (5개 각도별 점수)
3. **Merged Findings** (심각도순 요약)
4. **Executed Tests Result**
5. **Final Score & Decision** (Pass / Fix / Rework)

## Operational Policy

- **Tmux Isolation**: 모든 작업은 반드시 독립된 `tmux` 세션에서 Gemini CLI를 사용하여 수행한다.
- **Result-Oriented Retrieval**: 중간 과정의 모든 로그를 가져오지 않고, 최종 결과물과 핵심 검증 데이터만 수신하여 토큰 사용량을 극대화하여 절약한다.
- **No Review, No Completion**: 리뷰 없이 루프를 닫지 않는다.
- **No Summary, No Score**: 요약된 입력 근거 없이 점수를 매기지 않는다.
- **Max Iterations**: 품질 루프는 최대 3회까지 권장하며, 이후에도 해결되지 않으면 설계를 재검토한다.
