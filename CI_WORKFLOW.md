# CI_WORKFLOW.md

## Purpose

이 문서는 이 저장소에서 작업이 어떻게 시작되고, 검증되고, PR로 올라가고, merge 이후 어떻게 마감되는지를 정의하는 운영 기준서다.
핵심 목표는 아래와 같다.

- 계획 문서와 구현 작업이 분리되지 않게 한다.
- 사용자 승인 이후에만 구현 루프가 시작되게 한다.
- 구현 루프는 품질 점수와 리뷰를 통과해야만 종료되게 한다.
- PR과 merge 이후에는 계획 문서를 `completed`로 이동시켜 작업 상태가 저장소에 남게 한다.

이 문서는 실제 계획 원문을 담는 곳이 아니다.
계획 원문은 `docs/exec-plans/active`와 `docs/exec-plans/completed`에 둔다.

## Why This File Lives At Root

이 문서는 `docs/exec-plans` 안이 아니라 저장소 루트에 둔다.

- `docs/exec-plans`는 개별 실행 계획 파일이 들어가는 작업 저장소다.
- 이 문서는 그 계획 파일들이 어떤 생명주기를 따라야 하는지 정의하는 상위 운영 기준이다.
- 범위가 프론트/백엔드/보안/리뷰/PR/merge 전체를 걸치므로 루트 문서가 더 자연스럽다.

정리하면:

- 루트 `CI_WORKFLOW.md`: 프로세스 기준
- `docs/exec-plans/active`: 진행 중 계획
- `docs/exec-plans/completed`: merge 완료 후 보관 계획

## Related Documents

- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
- [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

## Directory Policy

이 워크플로우에서 각 위치의 역할은 아래와 같이 고정한다.

### 루트 문서

- `CI_WORKFLOW.md`: 작업 운영 플로우
- `QUALITY_SCORE.md`: 리뷰 점수와 루프 종료 조건
- 나머지 루트 문서: 구현 기준

### 계획 문서 저장 위치

- `docs/exec-plans/active`
  승인되어 현재 진행 중인 계획 문서
- `docs/exec-plans/completed`
  merge가 끝나 작업이 닫힌 계획 문서

강한 규칙:

- 승인 전 초안은 `docs/exec-plans/active`에 올리지 않아도 된다.
- 승인된 순간부터는 계획 문서를 `active`에 둔다.
- merge가 완료되면 같은 계획 문서를 `completed`로 이동한다.
- 완료된 계획 문서는 삭제하지 않고 기록으로 남긴다.

## Workflow Overview

전체 흐름은 아래 순서를 따른다.

1. 작업 요청 접수
2. 계획 수립
3. 계획 승인
4. 에이전트 구현 루프 시작
5. 테스트 실행
6. `QUALITY_SCORE.md` 기준 리뷰와 점수 계산
7. 점수 통과 시 PR 생성
8. CI 통과 및 merge
9. 계획 문서를 `completed`로 이동

이 흐름은 "계획 없는 구현", "리뷰 없는 종료", "merge 후 계획 방치"를 막기 위한 것이다.

## Stage 1. Request And Planning

작업이 시작되면 먼저 구현이 아니라 계획을 만든다.

계획 작성 기준:

- 계획 문서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)를 따른다.
- 계획은 한국어로 쓴다.
- 초보자가 계획서 하나만 읽고도 재개 가능해야 한다.
- 목적, 범위, 주요 파일, 검증 방법, 수용 기준이 반드시 있어야 한다.

권장 파일명 예:

- `docs/exec-plans/active/2026-04-15-backend-provider-refactor.md`
- `docs/exec-plans/active/2026-04-15-signup-flow-hardening.md`

승인 전 초안 단계에서는 임시 파일로 작성해도 되지만, 승인될 계획이라면 최종 파일명을 안정적으로 정한다.

## Stage 2. Plan Approval

계획은 반드시 사용자 승인 이후에만 구현 단계로 넘어간다.

승인 기준:

- 목적이 명확함
- 범위가 닫혀 있음
- 검증 방법이 적혀 있음
- 관련 기준 문서가 연결되어 있음

강한 규칙:

- 승인 전에는 본격 구현 브랜치를 밀지 않는다.
- 승인 전에는 "시도" 수준의 탐색과 조사만 허용한다.
- 승인되면 계획 문서는 `docs/exec-plans/active`에 위치해야 한다.

## Stage 3. Agent Loop

승인 이후부터는 에이전트 구현 루프를 시작한다.

기본 루프:

1. 계획 문서를 다시 읽는다.
2. 관련 기준 문서를 읽는다.
3. 구현 또는 수정한다.
4. 가능한 테스트를 실행한다.
5. `QUALITY_SCORE.md` 기준으로 리뷰한다.
6. 점수와 hard gate를 확인한다.
7. 통과면 루프 종료, 미달이면 수정 후 다시 돈다.

강한 규칙:

- 계획 문서와 코드 변경은 같이 진화해야 한다.
- 구현 중 발견 사항은 계획 문서에 반영해야 한다.
- 테스트 없이 "대충 끝났다"로 루프를 종료하지 않는다.
- 리뷰 없이 종료하지 않는다.

## Stage 4. Quality Gate

에이전트 루프의 종료 조건은 [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)를 따른다.

기본 종료 규칙:

- `final_score >= 85`
- hard gate 위반 없음
- 핵심 테스트 통과

기본 재작업 규칙:

- `final_score < 85`
- unresolved high/critical 있음
- 테스트 실패
- 관련 기준 문서와 명확히 충돌

리뷰는 `multi-angle-review` 성격의 독립 리뷰를 권장한다.

필수 관점:

- Readability
- Performance
- Security
- Test Quality
- Architecture

## Stage 5. Branch And PR

품질 게이트를 통과하면 PR 단계로 넘어간다.

권장 순서:

1. 작업 브랜치를 정리한다.
2. 변경 이유와 계획 문서 링크를 정리한다.
3. PR을 생성한다.
4. PR 본문에 아래를 포함한다.

- 작업 목적
- 계획 문서 경로
- 핵심 변경 사항
- 실행한 테스트
- 품질 점수 결과
- 남은 리스크

권장 브랜치 예:

- `backend-provider-boundary`
- `signup-validation-hardening`

PR 제목과 브랜치 이름은 반드시 작업 의미가 드러나야 한다.

허용 원칙:

- 어떤 기능이나 문제를 다루는지 한눈에 보여야 한다.
- 무엇을 바꾸는지 동사 또는 결과가 드러나야 한다.
- 가능하면 계획 문서의 주제와 이름이 자연스럽게 연결되어야 한다.

금지 예:

- `branch-12`
- `issue-work`
- `fix`
- `update`
- `pr`
- `work-in-progress`
- 의미 없는 날짜나 숫자만 있는 이름

허용 예:

- 브랜치: `backend-architecture-lint`
- 브랜치: `db-schema-sync-rule`
- PR 제목: `Add backend architecture lint with JSONL violations`
- PR 제목: `Enforce db-schema sync for backend persistence changes`

권장 PR 제목 형식:

- `<동사> <대상> <의도>`
- 예: `Add backend architecture lint for provider boundaries`
- 예: `Document exec-plan lifecycle from approval to merge`

강한 규칙:

- 품질 게이트를 통과하기 전에는 PR을 만들지 않는다.
- 계획 문서 경로 없이 PR을 만들지 않는다.
- PR 설명에는 어떤 기준 문서를 따랐는지 드러나야 한다.
- 브랜치 이름과 PR 제목은 작업 의미가 드러나야 하며, 자동 생성처럼 보이는 접두어나 무의미한 번호 이름을 금지한다.

## Stage 6. CI

PR이 생성되면 CI는 최소한 아래를 검증해야 한다.

### Required CI Checks

- 빌드 성공
- 백엔드 아키텍처 린트 성공
- 핵심 테스트 성공
- 린트 또는 정적 검증 성공
- 필요 시 문서 링크/형식 검증

이 저장소에 실제 CI 설정을 추가한다면 구현 위치는 다음을 기본으로 한다.

- `.github/workflows/ci.yml`
- 필요 시 `.github/workflows/pr-checks.yml`

현재 기본 워크플로는 `.github/workflows/ci.yml`에 둔다.
이 문서는 CI의 "정책"을 정의하고, 실제 YAML은 위 경로에서 이 정책을 구현한다.

### CI Contract

CI는 최소한 아래 상태를 구분해야 한다.

- `success`: merge 가능
- `failure`: 수정 후 다시 push 필요
- `cancelled`: 재실행 또는 원인 확인 필요

강한 규칙:

- CI 실패 상태에서 merge하지 않는다.
- flaky test라면 무시하지 말고 원인을 기록한다.
- 계획 문서에 검증 결과 요약을 남긴다.

## Stage 7. Merge

merge는 아래 조건이 만족될 때만 한다.

- PR 리뷰가 끝남
- CI 통과
- 품질 게이트 통과 상태 유지
- 범위 외 변경이 없음

merge 이후에는 작업이 끝난 것이 아니라 "문서 정리"까지 포함해 닫는다.

## Stage 8. Plan Completion

merge가 완료되면 계획 문서를 `docs/exec-plans/completed`로 이동한다.

이동 규칙:

1. 기존 `active` 계획 파일을 최신 상태로 갱신한다.
2. PR 번호, merge 날짜, 최종 결과를 계획 문서에 반영한다.
3. 파일을 `completed`로 이동한다.

권장 완료 메모:

- merge된 PR 링크
- 최종 반영 브랜치
- 핵심 검증 결과
- 남은 후속 작업이 있으면 별도 TODO

강한 규칙:

- merge됐는데 계획 문서가 `active`에 남아 있으면 안 된다.
- 완료 계획 문서는 후속 작업자가 읽을 수 있게 자기완결성을 유지해야 한다.

## State Machine

이 워크플로우의 상태는 아래처럼 해석한다.

- `draft`: 아직 승인 전 계획
- `approved`: 승인되었고 `active`에 있는 계획
- `implementing`: 에이전트 루프 수행 중
- `review-failed`: 품질 점수 또는 테스트 미달
- `pr-open`: PR 생성 완료
- `ci-failed`: CI 실패
- `ready-to-merge`: 리뷰와 CI 모두 통과
- `merged`: merge 완료
- `completed`: 계획 문서가 `completed`로 이동됨

권장 전이:

- `draft -> approved`
- `approved -> implementing`
- `implementing -> review-failed`
- `review-failed -> implementing`
- `implementing -> pr-open`
- `pr-open -> ci-failed`
- `ci-failed -> implementing`
- `pr-open -> ready-to-merge`
- `ready-to-merge -> merged`
- `merged -> completed`

## Recommended PR Template Content

PR 본문에는 최소한 아래 항목이 있으면 좋다.

- 요약
- 계획 문서 경로
- 관련 기준 문서
- 테스트 실행 결과
- 품질 점수
- 남은 리스크

예시:

```text
## Summary
- ...

## Plan
- docs/exec-plans/active/2026-04-15-backend-provider-refactor.md

## Standards
- BACKEND.md
- QUALITY_SCORE.md

## Tests
- ./gradlew clean test

## Quality Score
- Final Score: 88
- Hard Gate: none
```

## Failure Handling

### 계획이 승인되지 않은 경우

- 구현 루프를 시작하지 않는다.
- 계획을 보완하고 다시 승인받는다.

### 품질 점수가 미달인 경우

- `QUALITY_SCORE.md` 기준으로 수정한다.
- 같은 루프를 다시 돈다.

### PR은 열렸지만 CI가 실패한 경우

- 상태를 `implementing`으로 되돌린다.
- 원인을 수정하고 다시 테스트 및 리뷰 후 push한다.
- 백엔드 아키텍처 린트 실패라면 `backend/build/reports/architecture-lint/violations.jsonl` 또는 CI artifact를 읽어 `message`와 `suggestedFix`를 다음 수정 입력으로 사용한다.

### merge 없이 작업이 종료된 경우

- 계획 문서는 `active`에 남긴다.
- 문서 상단이나 회고 섹션에 중단 이유를 남긴다.

## Default Agent Policy

에이전트는 기본적으로 아래 정책을 따른다.

- 계획 없이 구현하지 않는다.
- 승인 없이 본격 구현하지 않는다.
- 리뷰 없이 PR을 만들지 않는다.
- 품질 점수 미달 상태에서 루프를 종료하지 않는다.
- merge 후 계획 문서를 `completed`로 옮기기 전까지 작업이 끝난 것으로 간주하지 않는다.

## Acceptance Criteria For This Workflow

이 문서가 실제로 잘 작동한다고 판단할 기준은 아래와 같다.

- 새 작업이 시작되면 계획 파일 위치가 명확하다.
- 승인 전/후 행동이 구분된다.
- 품질 점수와 hard gate가 루프 종료 조건으로 사용된다.
- PR 생성 시 필요한 정보가 누락되지 않는다.
- merge 후 계획 파일이 `completed`로 이동한다.

즉, 이 문서를 처음 읽은 사람도 "계획 작성 -> 승인 -> 구현 루프 -> 리뷰 -> PR -> merge -> completed 이동" 순서를 헷갈리지 않아야 한다.
