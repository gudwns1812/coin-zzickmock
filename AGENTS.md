# AGENTS.md

이 문서는 이 저장소를 작업하는 에이전트를 위한 맵이자 인덱스다.
직접 구현 규칙을 길게 담기보다, "어떤 작업을 할 때 어디를 먼저 읽어야 하는지"를 안내하는 입구 문서로 사용한다.

## How To Use This File

작업을 시작할 때 아래 순서로 판단한다.

0. 사용자 지시가 구현, 수정, 리뷰, 계획, PR, merge, 테스트, 품질 게이트와 연결되면 먼저 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)부터 읽는다.
1. 작업 범위가 루트 구조 판단인지, 프론트엔드인지, 백엔드인지, 보안인지 확인한다.
2. 이 문서에서 해당하는 기준 문서를 찾는다.
3. `CI_WORKFLOW.md`가 적용되는 작업이면 해당 흐름을 선행한 뒤 관련 기준 문서를 읽는다.
4. 기준 문서를 먼저 읽고, 필요하면 관련 워크스페이스 문서나 `docs/` 하위 자료로 내려간다.
5. 구현보다 문서 탐색이 먼저 필요한 작업에서는 이 문서를 목차처럼 사용한다.

이 파일은 규칙의 원문이 아니라 "어디에 원문이 있는지"를 알려주는 안내판이다.

## Mandatory Workflow Gate

이 저장소에서 사용자 지시가 실제 작업 산출물로 이어지는 순간, `CI_WORKFLOW.md`는 선택 문서가 아니라 강제 진입 게이트다.

강한 규칙:

- 구현, 버그 수정, 리팩터링, 테스트 추가, 리뷰, PR 준비, merge 준비 요청은 모두 먼저 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 탄다.
- 프론트엔드, 백엔드, 보안 문서는 `CI_WORKFLOW.md`를 지난 다음에 읽는 세부 기준 문서다.
- 계획 없는 바로 구현, 리뷰 없는 종료, 품질 게이트 없는 마감은 허용하지 않는다.
- 품질 게이트를 통과한 작업은 특별한 중단 지시가 없으면 PR 생성 단계까지 이어서 수행해야 한다.
- 테스트와 리뷰를 통과했더라도 PR URL이 없으면 완료로 보고하지 않는다.
- 브랜치 이름과 PR 제목은 사용자 naming 선호를 최우선으로 따르며, 자동 생성처럼 보이는 접두사는 쓰지 않는다.
- 사용자가 별도 예외를 주지 않았다면 PR 제목과 PR 본문은 반드시 한국어로 작성한다. 코드, 명령어, 경로 같은 식별자만 원문 표기를 유지한다.
- 예외는 저장소 구조 설명, 단순 문서 링크 안내, 구현을 동반하지 않는 짧은 사실 확인처럼 코드 변경과 무관한 요청뿐이다.
- 사용자가 "바로 고쳐"처럼 짧게 지시해도 워크플로우 생략으로 해석하지 않는다.
- 관련 루트 기준 문서나 상세 설계 문서가 방금 바뀌었다면, 기존 `active` 계획을 그대로 구현 근거로 쓰지 않는다.
- 기존 `active` 계획이 현재 기준과 충돌하면 먼저 계획을 갱신하거나 닫고, 그 다음에 구현에 들어간다.

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
  리뷰 시 반드시 사용하는 `multi-angle-review` 방식의 독립 리뷰를 점수화하고, 사용자 지시 범위만 검토 대상으로 삼는 품질 게이트 기준 문서.

### 작업 운영 플로우

- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
  계획 수립, 승인, 에이전트 루프, `red -> green -> refactor` 기반 TDD, 단계별 sub-agent 사용 원칙, 품질 게이트, PR, CI, merge, `completed` 이동과 의미 있는 브랜치/PR 이름 규칙, 그리고 작업 마무리 응답을 매우 짧게 유지하는 원칙까지 포함한 저장소 운영 플로우 기준 문서.

### 배포와 릴리즈 기준

- [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)
  배포, 릴리즈, 롤백, 환경, 산출물, 운영 기록 기준을 잡는 입구 문서.

### 계획과 진행 맥락

- [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
  작업 계획, 구현 맥락, 진행 중인 생각의 흔적을 확인할 때 먼저 보는 문서.

## `docs/` Map

`docs/`는 세부 자료 저장소다.
루트 문서를 읽고 더 깊게 들어가야 할 때 이동한다.

### 디자인 문서

- [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
  상세 설계 문서 묶음의 루트 인덱스.
- [docs/design-docs/ui-design](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design)
  UI 디자인 세부 문서 묶음. 시각 원칙, 레이아웃, 데이터 표시, 입력, 접근성 기준은 이 폴더 안에서 찾는다.
- [docs/design-docs/backend-design](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design)
  백엔드 상세 설계 문서 묶음. 레이어 구조, `Providers`, 린트, DB 스키마 동기화 기준은 이 폴더 안에서 찾는다.

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

### 배포와 릴리즈 문서

- [docs/release-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/README.md)
  배포/릴리즈 상세 문서 묶음의 루트 인덱스.
- [docs/release-docs](/Users/hj.park/projects/coin-zzickmock/docs/release-docs)
  환경, 체크리스트, 롤아웃/롤백, 릴리즈 노트 템플릿은 이 폴더 안에서 찾는다.

### 제품 명세

- [docs/product-specs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/README.md)
  제품 요구사항과 기능 명세 문서의 입구 문서.
- [docs/product-specs](/Users/hj.park/projects/coin-zzickmock/docs/product-specs)
  MVP, 화면 명세, 시뮬레이션 규칙 같은 제품 문서는 이 폴더 안에서 찾는다.

### 참고 자료

- [docs/references](/Users/hj.park/projects/coin-zzickmock/docs/references)
  조사 메모, 외부 레퍼런스, ADR 성격 문서를 두는 자리.
- [docs/references/README.md](/Users/hj.park/projects/coin-zzickmock/docs/references/README.md)
  참고 자료 디렉터리의 입구 문서.
- [docs/references/bitget](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget)
  Bitget API 참고자료 모음. 상품 메타데이터, 펀딩비, REST/WebSocket 연결 가이드는 이 폴더 안에서 찾는다.
- [docs/references/bitget/websocket](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/websocket)
  Bitget 공개 WebSocket 채널 참고자료 묶음. 캔들, 체결, 티커 스트림 문서는 이 폴더 안에서 찾는다.

## Task-based Navigation

### 저장소 구조를 이해해야 할 때

1. [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
2. [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
3. 설계 문서 구조가 필요하면 [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)

### 프론트엔드 화면, 상태, API 호출을 수정할 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
3. [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
4. 필요하면 [docs/design-docs/ui-design](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/ui-design) 안에서 세부 기준을 찾는다.

### 백엔드 구조나 새 기능 패키지를 잡을 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
3. [docs/design-docs/backend-design](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design)
4. 보안이 걸리면 [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

### 백엔드에서 DB를 읽거나 수정할 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
3. [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
4. 스키마를 읽을 때는 `db-schema.md`를 먼저 참고
5. 스키마 변경이 있으면 `backend/src/main/resources/db/migration` 아래에 새 `Flyway` 버전 파일을 만들고 코드와 문서를 함께 갱신

### 인증, 인가, 민감 정보, 외부 연동 보안을 다룰 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)
3. 프론트 작업이면 [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
4. 백엔드 작업이면 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)

### Bitget 시세, 상품 메타데이터, 외부 거래소 연동을 다룰 때

1. [docs/references/README.md](/Users/hj.park/projects/coin-zzickmock/docs/references/README.md)
2. [docs/references/bitget](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget)
3. WebSocket 채널 스펙이 필요하면 [docs/references/bitget/websocket](/Users/hj.park/projects/coin-zzickmock/docs/references/bitget/websocket)
4. 작업 성격에 맞는 세부 문서는 Bitget 폴더 안에서 찾는다.

### 종료 조건이나 리뷰 점수 기준이 필요할 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
3. 관련 구현 기준 문서
4. 사용자 지시 범위에 한해서 `multi-angle-review` 방식으로 독립 리뷰 수행

### 배포 준비, 릴리즈 절차, 롤백 기준이 필요할 때

1. [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)
2. [docs/release-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/README.md)
3. 상황에 맞는 상세 문서는 [docs/release-docs](/Users/hj.park/projects/coin-zzickmock/docs/release-docs) 안에서 찾는다.

### 계획 승인부터 PR, merge, completed 이동까지의 흐름이 필요할 때

1. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
2. [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
3. [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)

### 진행 중인 계획이나 이전 맥락을 확인할 때

1. [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
2. [docs/exec-plans/active](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active)
3. [docs/exec-plans/completed](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed)
4. 최근 종료 또는 대체된 맥락은 [docs/exec-plans/completed/2026-04-16-coin-futures-mvp-foundation.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed/2026-04-16-coin-futures-mvp-foundation.md)

### 디자인 기준이나 UI 세부 원칙이 필요할 때

1. [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
2. [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
3. 필요한 세부 주제 문서는 `ui-design` 또는 `backend-design` 폴더 안에서 찾는다.

## Current Gaps

현재 인덱스 관점에서 보이는 빈칸은 아래와 같다.

- `backend/HELP.md`는 실질적인 프로젝트 안내 문서가 아니라 기본 생성 문서다.

이런 영역에 문서가 추가되면, `AGENTS.md`에는 원문을 복제하지 말고 링크와 짧은 설명만 추가한다.

## Maintenance Rule For This File

이 파일은 문서의 "목차"만 관리한다.

- 새 기준 문서가 루트에 생기면 여기에 링크를 추가한다.
- `docs/` 아래에 대표 엔트리 문서가 생기면 여기에 연결한다.
- 구현 규칙의 상세 원문은 각 문서에 두고, 이 파일에는 요약만 둔다.
- 하위 폴더 문서는 가능한 한 폴더나 `README` 단위로 묶고, 정말 먼저 읽어야 하는 예외만 개별 파일로 연결한다.
- 링크가 깨지면 가장 먼저 이 파일부터 갱신한다.
