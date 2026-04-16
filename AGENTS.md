# AGENTS.md

이 문서는 이 저장소를 작업하는 에이전트를 위한 맵이자 인덱스다.
직접 구현 규칙을 길게 담기보다, "어떤 작업을 할 때 어디를 먼저 읽어야 하는지"를 안내하는 입구 문서로 사용한다.

## How To Use This File

작업을 시작할 때 아래 순서로 판단한다.

1. 작업 범위가 루트 구조 판단인지, 프론트엔드인지, 백엔드인지, 보안인지 확인한다.
2. 이 문서에서 해당하는 기준 문서를 찾는다.
3. 기준 문서를 먼저 읽고, 필요하면 관련 워크스페이스 문서나 `docs/` 하위 자료로 내려간다.
4. 구현보다 문서 탐색이 먼저 필요한 작업에서는 이 문서를 목차처럼 사용한다.

이 파일은 규칙의 원문이 아니라 "어디에 원문이 있는지"를 알려주는 안내판이다.

## Top-level Map

루트 문서들은 오래 유지할 기준 문서다.
세부 참고 자료는 `docs/` 아래에 둔다.

### 저장소 입구

- [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
  저장소 개요, 스택, 실행 방법, 현재 문서 연결의 출발점.

### 전체 구조와 책임 경계

- [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
  저장소 전체 구조, 루트와 워크스페이스의 책임 분리, `docs/`의 역할.

### 설계 문서 틀

- [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
  루트 기준 문서와 `docs/design-docs/` 상세 설계를 어떻게 나눌지 정하는 설계 문서의 틀.

### 프론트엔드 기준

- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
  프론트엔드 구현 원칙, 상태 관리, 데이터 패칭, 스타일링, 접근성, 에러 처리 기준.
- [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
  프론트 워크스페이스의 실제 codemap, 주요 라우트, 엔트리포인트, 폴더 역할.

### 백엔드 기준

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
  백엔드 작업 기준과 상세 설계 입구 문서.
- [backend/HELP.md](/Users/hj.park/projects/coin-zzickmock/backend/HELP.md)
  현재는 Spring Initializr 기본 문서에 가깝다. 아키텍처 기준 문서로는 사용하지 않는다.

### 보안 기준

- [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)
  인증, 인가, 입력 검증, 민감 정보 처리, 로그/외부 연동 보안 기준.

### 품질 게이트 기준

- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
  `multi-angle-review` 방식의 독립 리뷰를 점수화해서 에이전트 루프의 종료 조건과 재시도 조건으로 쓰는 기준 문서.

### 작업 운영 플로우

- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
  계획 수립, 승인, 에이전트 루프, 품질 게이트, PR, CI, merge, `completed` 이동과 의미 있는 브랜치/PR 이름 규칙까지 포함한 저장소 운영 플로우 기준 문서.

### 계획과 진행 맥락

- [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
  작업 계획, 구현 맥락, 진행 중인 생각의 흔적을 확인할 때 먼저 보는 문서.

## `docs/` Map

`docs/`는 세부 자료 저장소다.
루트 문서를 읽고 더 깊게 들어가야 할 때 이동한다.

### 디자인 문서

- [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
  상세 설계 문서 묶음의 루트 인덱스.
- [docs/design-docs/ui-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/README.md)
  UI 디자인 문서 묶음의 입구.
- [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
  백엔드 상세 설계 문서 묶음의 입구.
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
  백엔드 구조, 레이어, `Providers`, 린트, DB 스키마 동기화 기준의 상세 원문.
- [docs/design-docs/ui-design/01-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/01-foundations.md)
  시각적 기초 원칙.
- [docs/design-docs/ui-design/02-layouts-and-surfaces.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/02-layouts-and-surfaces.md)
  레이아웃과 표면 설계 기준.
- [docs/design-docs/ui-design/03-data-display.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/03-data-display.md)
  데이터 표시 방식 기준.
- [docs/design-docs/ui-design/04-inputs-and-overlays.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/04-inputs-and-overlays.md)
  입력 요소와 오버레이 설계 기준.
- [docs/design-docs/ui-design/05-motion-states-accessibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/05-motion-states-accessibility.md)
  모션, 상태, 접근성 기준.
- [docs/design-docs/ui-design/06-component-boundaries.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design/06-component-boundaries.md)
  컴포넌트 경계와 분리 기준.

### 실행 계획

- [docs/exec-plans](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans)
  구현 계획 저장소.
- [docs/exec-plans/active](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active)
  진행 중인 계획 문서 위치.
- [docs/exec-plans/completed](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed)
  완료된 계획 문서 위치.

### 생성 산출물

- [docs/generated](/Users/hj.park/projects/coin-zzickmock/docs/generated)
  생성형 산출물이나 자동 생성 문서를 두는 자리.
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
  백엔드에서 DB를 참고하거나 수정할 때 함께 확인하고, 스키마가 바뀌면 같이 갱신해야 하는 기준 문서.

### 제품 명세

- [docs/product-specs](/Users/hj.park/projects/coin-zzickmock/docs/product-specs)
  제품 요구사항, 기능 명세 문서를 두는 자리.
  현재는 엔트리 문서가 보이지 않으므로, 실제 파일이 생기면 여기에서 연결을 확장한다.

### 참고 자료

- [docs/references](/Users/hj.park/projects/coin-zzickmock/docs/references)
  조사 메모, 외부 레퍼런스, ADR 성격 문서를 두는 자리.
  현재는 엔트리 문서가 보이지 않으므로, 실제 파일이 생기면 여기에서 연결을 확장한다.

## Task-based Navigation

### 저장소 구조를 이해해야 할 때

1. [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
2. [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
3. 설계 문서 구조가 필요하면 [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)

### 프론트엔드 화면, 상태, API 호출을 수정할 때

1. [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
2. [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
3. 필요하면 `docs/design-docs/ui-design/*`

### 백엔드 구조나 새 기능 패키지를 잡을 때

1. [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
2. [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
3. 보안이 걸리면 [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

### 백엔드에서 DB를 읽거나 수정할 때

1. [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
2. [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
3. 스키마 변경이 있으면 코드와 문서를 함께 갱신

### 인증, 인가, 민감 정보, 외부 연동 보안을 다룰 때

1. [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)
2. 프론트 작업이면 [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
3. 백엔드 작업이면 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)

### 종료 조건이나 리뷰 점수 기준이 필요할 때

1. [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
2. 관련 구현 기준 문서
3. 필요하면 `multi-angle-review` 방식으로 독립 리뷰 수행

### 계획 승인부터 PR, merge, completed 이동까지의 흐름이 필요할 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
3. [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)

### 진행 중인 계획이나 이전 맥락을 확인할 때

1. [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
2. `docs/exec-plans/active`
3. `docs/exec-plans/completed`

### 디자인 기준이나 UI 세부 원칙이 필요할 때

1. [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
2. [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
3. 필요한 세부 주제 문서

## Current Gaps

현재 인덱스 관점에서 보이는 빈칸은 아래와 같다.

- `docs/product-specs/`에는 아직 연결할 대표 문서가 보이지 않는다.
- `docs/references/`에도 아직 연결할 대표 문서가 보이지 않는다.
- `backend/HELP.md`는 실질적인 프로젝트 안내 문서가 아니라 기본 생성 문서다.

이런 영역에 문서가 추가되면, `AGENTS.md`에는 원문을 복제하지 말고 링크와 짧은 설명만 추가한다.

## Maintenance Rule For This File

이 파일은 문서의 "목차"만 관리한다.

- 새 기준 문서가 루트에 생기면 여기에 링크를 추가한다.
- `docs/` 아래에 대표 엔트리 문서가 생기면 여기에 연결한다.
- 구현 규칙의 상세 원문은 각 문서에 두고, 이 파일에는 요약만 둔다.
- 링크가 깨지면 가장 먼저 이 파일부터 갱신한다.
