# DESIGN.md

## Purpose

이 문서는 이 저장소에서 설계를 어떤 층위로 기록하고, 어디에 두고, 어떻게 갱신할지 정하는 설계 문서의 기준 틀이다.

목표는 세 가지다.

- 기준서와 설계 원문을 섞지 않는다.
- 루트 문서는 짧고 강한 입구 문서로 유지한다.
- 상세 설계는 `docs/design-docs/` 아래로 내려서 주제별로 확장 가능하게 만든다.

## Document Layers

이 저장소의 설계 관련 문서는 아래 3층으로 나눈다.

### 1. 루트 기준 문서

루트 문서는 오래 유지할 입구이자 기준점이다.

- [ARCHITECTURE.md](/Users/hj.park/projects/coin-zzickmock/ARCHITECTURE.md)
- [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)

이 문서들은 "어디를 먼저 읽어야 하는지"와 "무엇을 절대 어기면 안 되는지"를 빠르게 전달해야 한다.
너무 많은 상세 설계 원문까지 떠안지 않는다.

### 2. 상세 설계 문서

상세 설계는 `docs/design-docs/` 아래에 둔다.

- 주제별로 디렉터리를 나눈다.
- 여러 문서로 나눠도 된다.
- 배경, 경계, 예시, 세부 패키지 구조, 검증 방식까지 담는다.

예:

- `docs/design-docs/ui-design/`
- `docs/design-docs/backend-design/`

### 3. 생성/운영 산출물

설계 참고에 필요한 생성 산출물은 `docs/generated/` 아래에 둔다.

예:

- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)

이 문서는 설계 원문이라기보다 설계를 검증하거나 구현할 때 참조해야 하는 사실 기반 산출물이다.

## Placement Rules

### 루트에 두는 것

아래 성격의 문서는 루트에 둔다.

- 저장소 전체에서 자주 참조되는 기준
- 구현 전에 먼저 읽어야 하는 입구 문서
- 특정 기술이 아니라 작업 방식 전체를 지배하는 규칙

예:

- `BACKEND.md`는 백엔드 작업 기준과 입구 문서
- `CI_WORKFLOW.md`는 작업 운영 플로우 기준
- `QUALITY_SCORE.md`는 리뷰 종료 기준

### `docs/design-docs/`에 두는 것

아래 성격의 문서는 `docs/design-docs/`에 둔다.

- 설계의 상세 근거
- 레이어별 책임 설명
- 패키지 구조 예시
- 교차 관심사 설계
- 설계 의사결정을 뒷받침하는 세부 문서

즉, 설명이 길어지고 예시가 늘어나기 시작하면 루트 문서보다 `docs/design-docs/`가 더 맞다.

## Recommended Shape For Design Docs

상세 설계 문서는 가능하면 아래 순서를 따른다.

1. 목적
2. 문제 또는 배경
3. 핵심 결정
4. 경계와 책임
5. 구조 예시
6. 구현 시 강제 규칙
7. 검증 방법
8. 관련 문서

문서가 길어지면 `01-`, `02-` 식으로 쪼개고, 디렉터리 안에 `README.md`를 둔다.

강한 규칙:

- 한 상세 설계 문서는 하나의 1차 책임만 맡는다.
- 구조, Provider, DB, 테스트, 린트처럼 읽는 목적이 다른 규칙을 한 파일에 계속 누적하지 않는다.
- 기존 번호 문서의 책임에 맞지 않는 규칙이 생기면 새 번호 문서를 추가한다.

## Current Repository Policy

현재 저장소에서는 아래처럼 해석한다.

- `BACKEND.md`: 백엔드 작업 기준과 상세 설계 입구
- `docs/design-docs/backend-design/`: 백엔드 상세 설계 원문
- `FRONTEND.md`: 프론트 작업 기준
- `docs/design-docs/ui-design/`: UI 상세 설계 원문

즉, 백엔드 설계 원문이 길어질수록 `BACKEND.md`를 더 키우는 대신 `docs/design-docs/backend-design/`으로 내린다.
또한 `docs/design-docs/backend-design/`과 같은 상세 설계 디렉터리는 `README.md`를 중심으로 책임별 번호 문서를 안내해야 하며, 다시 단일 거대 원문 파일 구조로 돌아가지 않는다.

## Maintenance Rule

- 루트 문서는 짧고 결정적인 링크 허브로 유지한다.
- 상세 설계를 추가하거나 크게 바꾸면 `docs/design-docs/README.md`도 같이 갱신한다.
- `BACKEND.md`, `FRONTEND.md` 같은 기준 문서는 상세 설계의 존재를 반드시 링크한다.
- 설계와 사실 산출물이 함께 변하면 `docs/generated/` 문서도 같이 갱신한다.
- 상세 설계 규칙을 수정할 때는 해당 디렉터리 `README.md`와 관련 루트 입구 문서의 링크 흐름이 여전히 맞는지 같이 확인한다.
