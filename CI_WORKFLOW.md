# CI_WORKFLOW.md

## Purpose

이 문서는 이 저장소에서 작업이 어떻게 시작되고, 검증되고, PR로 올라가고, merge 이후 어떻게 마감되는지를 정의하는 운영 기준서다.
핵심 목표는 아래와 같다.

- 계획 문서와 구현 작업이 분리되지 않게 한다.
- 사용자 승인 이후에만 구현 루프가 시작되게 한다.
- 구현 루프는 품질 점수와 리뷰를 통과해야만 종료되게 한다.
- PR과 merge 이후에는 계획 문서를 `completed`로 이동시켜 작업 상태가 저장소에 남게 한다.

강한 규칙:

- 구현성 작업은 모두 이 문서부터 시작한다.
- 사용자 지시가 짧거나 급해 보여도 이 흐름을 생략하지 않는다.
- 프론트엔드, 백엔드, 보안, 품질 문서는 이 문서를 지난 뒤 따라가는 하위 기준이다.
- 예외는 코드 변경이 없는 단순 안내, 링크 제공, 사실 확인 같은 비구현 요청뿐이다.
- 기준 문서가 바뀌었는데 기존 `active` 계획이 그 변화를 반영하지 못하면, 해당 계획은 stale 상태로 보고 먼저 갱신 또는 종료 처리한다.
- 품질 게이트를 통과한 구현 작업은 특별한 중단 지시가 없으면 `Stage 5`의 branch, commit, push, PR 생성까지 이어서 수행한다.
- 에이전트는 PR이 아직 없는데도 작업을 완료처럼 보고하지 않는다. PR이 없으면 URL 대신 blocker와 원인을 먼저 보고한다.

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

강제 진입 규칙:

- 사용자 요청이 코드, 테스트, 리뷰, 계획 문서, PR, merge 중 하나라도 건드리면 이 흐름에 자동으로 진입한다.
- "우선 고쳐", "일단 해", "바로 수정" 같은 표현은 속도 우선 신호이지 워크플로우 생략 허가가 아니다.
- 구현 에이전트는 작업 범위를 읽은 직후 현재 단계가 `Stage 1`부터 어디인지 사용자에게 짧게 공유하고 진행한다.
- 구현 직전에 참조하는 `active` 계획이 현재 루트 기준 문서와 충돌하면, 구현을 멈추고 `Stage 1`로 돌아가 계획을 다시 맞춘다.

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

stale 계획 처리 규칙:

- 루트 기준 문서나 상세 설계 원문이 바뀌어 기존 `active` 계획이 더 이상 현재 기준을 설명하지 못하면 stale로 본다.
- stale 계획은 그대로 구현 근거로 쓰지 않는다.
- stale 계획을 계속 사용할지, 갱신할지, 종료할지는 사용자 지시를 우선한다.
- stale 계획을 닫는다면 이유를 계획 문서에 남기고 `completed`로 이동할 수 있다.

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

### TDD Implementation Rule

코드 구현 작업은 기본적으로 `TDD`로 진행한다.
구현 순서는 반드시 `red -> green -> refactor`를 따른다.

단계 정의:

1. `red`
   실패하는 테스트를 먼저 만든다.
2. `green`
   `red`에서 만든 실패 테스트를 통과시키는 최소 구현만 넣는다.
3. `refactor`
   테스트가 모두 통과하는 `green` 상태에서만 구조와 가독성을 개선한다.

강한 규칙:

- `red`는 "빌드 실패"를 의미하지 않는다.
- `red` 단계의 실패는 의도한 동작을 검증하는 테스트 실패여야 한다.
- 컴파일 에러, 설정 오류, 깨진 테스트 픽스처를 `red`로 간주하지 않는다.
- 새 동작이나 회귀 위험이 있는 수정은 먼저 해당 동작을 고정하는 테스트부터 추가한다.
- `green` 단계에서는 테스트를 통과시키는 데 필요한 최소 변경만 허용한다.
- `refactor` 단계는 테스트가 모두 통과하는 상태에서만 시작한다.
- `refactor`에서는 동작 변경보다 구조 개선, 책임 분리, 이름 개선, 중복 제거를 우선한다.

### TDD Sub-agent Rule

구현 작업에서는 단계별 전용 sub-agent를 사용해 `red`, `green`, `refactor`를 분리한다.
순서는 병렬이 아니라 직렬이다.

권장 순서:

1. `red` sub-agent가 실패하는 테스트를 만든다.
2. 메인 에이전트가 테스트 실패가 의도된 실패인지 확인한다.
3. `green` sub-agent가 해당 테스트를 통과시키는 최소 구현을 만든다.
4. 메인 에이전트가 관련 테스트를 다시 실행해 모두 통과하는지 확인한다.
5. `refactor` sub-agent가 아키텍처와 가독성을 개선한다.
6. 메인 에이전트가 전체 테스트와 품질 게이트를 다시 확인한다.

예외:

- 문서 전용 작업
- 의미 있는 동작 변화가 없는 단순 문자열 수정
- 테스트 대상이 없는 운영 메타데이터 정리

위 예외가 아니라면, 구현 전에 `red`를 먼저 만드는 흐름을 생략하지 않는다.

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

리뷰는 반드시 `multi-angle-review` 방식의 독립 리뷰로 수행한다.

범위 규칙:

- 리뷰 기본 범위는 사용자가 지시한 diff, 파일, 경로, 기능 범위만 사용한다.
- 범위 지시가 없다고 해서 현재 working tree 전체나 저장소 전체를 기본값으로 잡지 않는다.
- 전체 범위 리뷰는 사용자가 명시적으로 요청한 경우에만 수행한다.

필수 관점:

- Readability
- Performance
- Security
- Test Quality
- Architecture

## Stage 5. Branch And PR

품질 게이트를 통과하면 PR 단계로 넘어간다.

기본 원칙:

- 품질 게이트 통과 후의 기본 동작은 "PR 생성까지 진행"이다.
- 사용자가 명시적으로 "PR은 만들지 마", "커밋 전까지만", "로컬 수정만"처럼 멈춤 지점을 준 경우에만 예외로 멈춘다.
- 에이전트는 검증 성공 보고에서 멈추지 않고, branch 정리, commit, push, PR 생성까지 이어서 처리한다.
- PR을 실제로 만들 수 없는 경우에만 예외로 멈추며, 그때는 완료 보고 대신 blocker를 먼저 적는다.

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

### Dirty Worktree Rule

현재 워킹트리에 사용자 작업이나 다른 기능 변경이 섞여 있으면, 에이전트는 PR 생성 전에 먼저 범위를 분리해야 한다.

허용 전략:

1. 내 변경 파일만 선택적으로 stage 해서 commit/PR 생성
2. 새 브랜치를 만든 뒤 내 변경만 옮겨서 commit/PR 생성
3. 내 변경과 기존 변경이 같은 파일에서 충돌해 안전한 분리가 불가능하면, 그 사실을 blocker로 보고하고 사용자 확인을 받음

강한 규칙:

- 사용자 또는 다른 작업의 unrelated 변경을 함께 올리는 PR은 금지한다.
- 범위 분리가 끝나기 전에는 PR 생성 완료로 간주하지 않는다.
- dirty worktree는 PR 생략의 이유가 아니라, 먼저 해결해야 하는 전처리 단계다.

권장 브랜치 예:

- `backend-provider-boundary`
- `signup-validation-hardening`

PR 제목과 브랜치 이름은 반드시 작업 의미가 드러나야 한다.

허용 원칙:

- 어떤 기능이나 문제를 다루는지 한눈에 보여야 한다.
- 무엇을 바꾸는지 동사 또는 결과가 드러나야 한다.
- 가능하면 계획 문서의 주제와 이름이 자연스럽게 연결되어야 한다.
- 사용자가 브랜치 이름이나 PR 제목 선호를 명시했다면 그 지시를 최우선으로 따른다.
- 외부 tool, agent skill, 전역 기본값이 다른 naming 규칙을 권장하더라도 이 저장소에서는 사용자 지시와 로컬 문서 규칙이 우선한다.

금지 예:

- `branch-12`
- `issue-work`
- `fix`
- `update`
- `pr`
- `work-in-progress`
- `codex/foo`
- `[codex] some title`
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
- 사용자에게 작업 완료를 알릴 때는 "결과보고"에서 끝내지 않고 PR 생성 여부를 먼저 기준으로 삼는다.
- PR URL이 없으면 작업은 아직 완료 상태가 아니다. 예외는 사용자 명시 중단 지시 또는 외부 blocker뿐이다.
- 브랜치 생성, commit, push, PR 생성 중 어디에서 멈췄는지 항상 단계 이름으로 명시한다.
- dirty worktree 때문에 멈추면 "어떤 파일이 범위 분리를 막았는지"를 함께 적는다.
- 작업 마무리 응답은 매우 짧게 쓴다. 토큰을 아끼기 위해 PR 상태, 핵심 테스트 결과, 남은 blocker가 있을 때만 짧게 적는다.
- PR 설명에는 어떤 기준 문서를 따랐는지 드러나야 한다.
- 브랜치 이름과 PR 제목은 작업 의미가 드러나야 하며, 자동 생성처럼 보이는 접두어나 무의미한 번호 이름을 금지한다.
- 사용자가 특정 접두사나 형식을 쓰지 말라고 지시했다면, 에이전트는 전역 기본값보다 그 지시를 우선 적용해야 한다.

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
또는 사용자 지시나 기준 변경으로 계획이 stale/superseded 상태가 되면, 종료 사유를 남기고 `docs/exec-plans/completed`로 이동할 수 있다.

이동 규칙:

1. 기존 `active` 계획 파일을 최신 상태로 갱신한다.
2. merge 완료 계획이면 PR 번호, merge 날짜, 최종 결과를 계획 문서에 반영한다.
3. stale/superseded 종료 계획이면 종료 이유, 대체 기준, 후속 진입점을 계획 문서에 반영한다.
4. 파일을 `completed`로 이동한다.

권장 완료 메모:

- merge된 PR 링크
- 최종 반영 브랜치
- 핵심 검증 결과
- 남은 후속 작업이 있으면 별도 TODO

강한 규칙:

- merge됐는데 계획 문서가 `active`에 남아 있으면 안 된다.
- stale 또는 superseded로 닫힌 계획도 `active`에 남겨 두지 않는다.
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
- `superseded`: 기준 변경 또는 사용자 지시로 더 이상 현재 구현 기준이 아닌 계획
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
- `approved -> superseded`
- `implementing -> superseded`
- `superseded -> completed`

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
