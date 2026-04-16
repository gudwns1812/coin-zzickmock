# ARCHITECTURE.md

이 문서는 이 저장소의 최상위 구조와 오래 유지될 책임 경계를 설명한다. 구현 디테일이나 파일 단위 코드 투어보다는, 새 기여자가 "어디에 무엇이 있고 무엇을 어디서 바꿔야 하는지"를 빠르게 파악하는 데 목적이 있다.

## System Overview

`coin-zzickmock`는 코인/주식 계열 모의 투자 경험을 다루는 workspace다.

- 실제 사용자 경험의 중심은 현재 `frontend/`다.
- `backend/`는 Spring Boot 기반 서비스이며, 장기적으로 도메인 규칙과 데이터의 최종 출처가 되어야 한다.
- 루트는 제품 기능을 직접 담기보다 workspace 실행, 문서, 모듈 연결을 담당한다.
- `docs/`는 런타임 코드가 아니라 제품 맥락, 계획, 참고 자료를 보관하는 공간이다.

## Repository Map

```text
coin-zzickmock/
├── frontend/        # 현재 사용자 경험의 중심인 Next.js 앱
├── backend/         # Spring Boot 서비스
├── docs/            # 제품 문서, 계획, 참고 자료
├── README.md        # 저장소 입구와 실행 방법
└── ARCHITECTURE.md  # 최상위 구조와 책임 경계
```

## Top-level Responsibilities

### Root

루트는 workspace 셸이다.

- 실행 진입점과 공통 패키지 연결을 제공한다.
- 모듈 간 관계를 보여 준다.
- 제품 문서와 구조 설명의 기준점을 둔다.

즉, 사용자 기능을 바꾸는 대부분의 작업은 루트보다 `frontend/` 또는 `backend/`에서 시작한다.

### `frontend/`

현재 제품의 주요 흐름을 담당하는 Next.js App Router 애플리케이션이다.

- 라우트와 화면 조립
- 인증/접근 제어의 1차 경계
- 사용자 상호작용과 클라이언트 상태
- 백엔드 또는 프록시 호출을 통한 데이터 표시

프론트 내부의 더 구체적인 codemap과 진입점은 [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)에 둔다.

### `backend/`

Spring Boot 기반 서비스다.

- 장기적으로 도메인 규칙의 최종 출처
- 영속성과 외부 연동의 중심
- 인증과 데이터 일관성 보장의 기준점

현재는 프론트보다 작은 상태지만, 구조적으로는 "보조 모듈"이 아니라 시스템의 진실한 규칙이 모여야 하는 자리다.

### `docs/`

런타임 바깥의 기억 장치다.

- `docs/product-specs/`: 제품 요구사항과 기능 사양
- `docs/references/`: 조사 내용과 외부 참고 자료
- `docs/exec-plans/`: 구현 계획과 진행 기록
- `docs/release-docs/`: 배포, 릴리즈, 롤백 운영 문서
- `docs/generated/`: 생성 산출물

코드를 어떻게 바꿀지보다 왜 바꾸는지를 확인하고 싶을 때 먼저 보는 곳이다.

## Stable Boundaries

이 저장소에서 오래 유지되어야 하는 핵심 경계는 아래와 같다.

### Frontend vs Backend

- 프론트는 표현, 흐름 제어, 사용자 상호작용을 담당한다.
- 백엔드는 도메인 규칙, 데이터 일관성, 인증의 최종 출처를 담당한다.

지금 구현 상태가 완전히 그렇지 않더라도, 새 작업은 이 방향을 더 강화해야 한다.

### Runtime Code vs Documentation

- `frontend/`, `backend/`는 실제 런타임 코드다.
- `docs/`와 루트 문서는 의사결정과 이해를 돕는 보조 수단이다.

문서는 코드의 대체물이 아니라 코드의 맥락을 설명하는 장치로 유지한다.

### Route/UI Composition vs Shared Logic

프론트 내부에서는 다음 구분을 유지하는 편이 좋다.

- 라우트와 레이아웃: 화면 흐름과 조립
- UI 컴포넌트: 렌더링 가능한 조각
- API/훅/스토어/유틸: 재사용 가능한 동작과 상태

세부 폴더 설명은 루트 문서보다 프론트 전용 문서에 두는 편이 유지보수에 유리하다.

## Architectural Invariants

- 현재 사용자 경험의 진입점은 루트가 아니라 `frontend/`다.
- URL 수준에서 판단 가능한 접근 제어는 가능한 한 요청 경계에서 먼저 처리한다.
- 프론트에 임시 규칙이 있더라도, 영구적인 비즈니스 규칙은 백엔드로 수렴해야 한다.
- 문서는 구조와 의도를 설명하되, 자주 바뀌는 구현 디테일을 과하게 복제하지 않는다.

## Where To Start

- 저장소 실행과 전체 맥락: [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
- 설계 문서 구조와 배치 기준: [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
- 배포와 릴리즈 운영 기준: [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)
- 프론트 구조와 실제 진입점: [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
- 백엔드 작업 기준: [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- 백엔드 상세 설계: [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- 제품 배경과 구현 맥락: [docs/](/Users/hj.park/projects/coin-zzickmock/docs)

## Out of Scope

이 문서는 각 화면의 UX 세부사항, API 스키마, 스타일 규칙, 개별 훅과 컴포넌트의 동작까지 설명하지 않는다. 그런 내용은 더 가까운 문서나 코드 옆 문서가 맡는다.
