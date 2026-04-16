# BACKEND.md

## Purpose

이 문서는 `coin-zzickmock` 백엔드 작업의 입구 문서다.
예전처럼 상세 설계 원문까지 이 파일 하나에 몰아넣지 않고, 작업 기준과 읽기 순서를 짧게 정리한다.

상세 설계는 아래 문서로 분리한다.

- [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
- [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

## What This File Does

이 문서는 아래 역할만 맡는다.

- 백엔드 작업 전에 무엇을 읽어야 하는지 알려 준다.
- 절대 놓치면 안 되는 강한 기준만 짧게 고정한다.
- DB, 린트, 설계 문서 같은 연관 문서를 연결한다.

즉, "백엔드 작업용 체크인 문서"라고 보면 된다.

## Read Order

### 구조나 새 기능 패키지를 설계할 때

1. [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
2. [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
3. 보안이 걸리면 [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

### DB를 읽거나 수정할 때

1. [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
2. [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
3. 스키마 변경이 있으면 코드와 `db-schema.md`를 함께 갱신

### 백엔드 품질 게이트나 린트를 확인할 때

1. [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
2. [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
3. [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

## Non-negotiables

아래는 상세 설계 문서로 내려가지 않더라도 기억해야 하는 핵심 규칙이다.

- 백엔드는 `feature-first`로 자른다.
- 레이어는 `api`, `application`, `domain`, `infrastructure`로 고정한다.
- 인증, 커넥터, 텔레메트리, 기능 플래그는 `Providers` 뒤로 숨긴다.
- DB를 바꾸면 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)도 같이 갱신한다.
- 백엔드 변경은 `./gradlew architectureLint`와 `./gradlew check`를 기준으로 검증한다.

## Architecture Lint

백엔드에는 프로젝트 전용 아키텍처 린터가 있다.

- 실행: `./gradlew architectureLint`
- 통합 검증: `./gradlew check`
- 리포트: `backend/build/reports/architecture-lint/violations.jsonl`

이 린터는 사람이 보는 용도만이 아니라, Codex가 로그를 다시 읽어 수정 루프를 돌리는 용도로도 사용한다.
로그 형식과 규칙의 상세 의미는 상세 설계 문서를 따른다.

## Completion Checklist

백엔드 작업을 끝냈다고 보기 위한 최소 조건은 아래와 같다.

- 관련 상세 설계를 읽고 반영했다.
- 필요한 경우 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 갱신했다.
- `./gradlew architectureLint`를 통과했다.
- `./gradlew check`를 통과했다.
- 품질 점수와 PR/CI 흐름은 [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md), [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따랐다.
