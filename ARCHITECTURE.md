# ARCHITECTURE.md

이 문서는 이 저장소의 최상위 구조와 오래 유지될 책임 경계를 설명한다. 구현 디테일이나 파일 단위 코드 투어보다는, 새 기여자가 "어디에 무엇이 있고 무엇을 어디서 바꿔야 하는지"를 빠르게 파악하는 데 목적이 있다.

## System Overview

`coin-zzickmock`는 코인 선물 모의 투자 경험을 다루는 workspace다.

- 실제 사용자 경험의 중심은 현재 `frontend/`다.
- `backend/`는 Spring Boot 기반 서비스이며, 장기적으로 도메인 규칙과 데이터의 최종 출처가 되어야 한다.
- 루트는 제품 기능을 직접 담기보다 workspace 실행, 문서, 모듈 연결을 담당한다.
- `docs/`는 런타임 코드가 아니라 제품 맥락, 계획, 참고 자료를 보관하는 공간이다.
- 이전 주식 화면/타입/컴포넌트가 일부 남아 있지만, 새 주요 흐름은 코인 선물 `markets`, `portfolio`, `mypage`, `watchlist`, `shop`, 관리자 보상 처리로 수렴 중이다.

## Repository Map

```text
coin-zzickmock/
├── frontend/        # 현재 사용자 경험의 중심인 Next.js 앱
├── backend/         # Spring Boot 서비스
├── docs/            # 제품 문서, 설계 기준, 계획, 참고 자료
├── FRONTEND.md      # 프론트 작업 기준과 읽기 순서
├── BACKEND.md       # 백엔드 작업 기준과 읽기 순서
├── RELEASE.md       # 배포/릴리즈 작업 기준과 읽기 순서
├── README.md        # 저장소 입구와 실행 방법
└── ARCHITECTURE.md  # 최상위 구조와 책임 경계
```

## Architecture Snapshot

현재 저장소는 단순한 프론트/백엔드 2분할이 아니라, 아래 구조를 함께 가진다.

### Runtime Shape

- 루트 `package.json`은 `frontend` npm workspace를 실행하는 셸이다.
- 프론트는 Next.js App Router 애플리케이션이며, 사용자가 직접 만나는 라우트와 화면 조립을 맡는다.
- 백엔드는 별도 Gradle/Spring Boot 서비스이며, `/api/futures/**` HTTP API와 SSE 스트림을 제공한다.
- 프론트는 서버 컴포넌트와 Next route handler에서 백엔드를 호출하고, 일부 SSE 경로는 Next API route가 백엔드 스트림을 프록시한다.

### Frontend Architecture

프론트의 현재 구조는 App Router 기반 화면 조립과 클라이언트 실시간 화면을 섞어 쓴다.

- `frontend/app/`: 라우트, 레이아웃, 오류 경계, Next route handler
- `frontend/middleware.ts`: 모바일 차단, 로그인 보호, 지원 심볼 검증 같은 요청 경계 규칙
- `frontend/lib/futures-api.ts`: 서버 쪽 백엔드 호출과 fallback 데이터 병합 경계
- `frontend/lib/markets.ts`: 현재 지원 심볼과 프론트 fallback market snapshot
- `frontend/components/router/`: 라우트 결합에 가까운 기존 컴포넌트 영역
- `frontend/components/futures/`: 코인 선물 거래 화면의 현재 주요 클라이언트 컴포넌트
- `frontend/components/ui/`, `frontend/components/ui/shared/`: 반복 UI 조각과 공용 조합
- `frontend/store/`: Zustand 기반 전역 UI/사용자 상태
- `frontend/hooks/`: 브라우저 상호작용과 반복 클라이언트 동작

현재 프론트에는 `stock` 관련 레거시 라우트와 컴포넌트가 남아 있다.
새 작업은 기존 구조를 갑자기 `src/`나 `features/`로 부분 이전하지 않고, 현재 codemap 안에서 코인 선물 흐름을 더 선명하게 만드는 방향을 따른다.

### Backend Architecture

백엔드는 `clean architecture lite`를 목표로 한 feature-first 구조다.
상세 원문은 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)와 [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)에 둔다.

핵심 구조:

```text
backend/src/main/java/coin/coinzzickmock/
  CoinZzickmockApplication.java
  common/
  providers/
  feature/
    <feature-name>/
      api/
      application/
      domain/
      infrastructure/
```

백엔드의 고정 규칙:

- 기능은 `feature/<feature-name>` 아래에서 수직으로 자른다.
- 기능 레이어는 `api`, `application`, `domain`, `infrastructure`로 고정한다.
- `api`는 HTTP 요청/응답, DTO 검증, application 호출, 응답 매핑을 맡는다.
- `application`은 유스케이스 오케스트레이션, command/query/result, repository/provider 계약 호출을 맡는다.
- `domain`은 프레임워크와 저장소를 모르는 비즈니스 규칙, 값, 정책, 상태 전이를 맡는다.
- `infrastructure`는 JPA, 외부 연동, mapper, config, framework adapter를 맡는다.
- 인증, 커넥터, 텔레메트리, 기능 플래그 같은 교차 관심사는 `providers/Providers.java` 경계 뒤로 모은다.
- 인터페이스는 기본값이 아니며, 실제 외부 경계나 다중 구현이 있을 때만 둔다.

현재 주요 feature는 `market`, `order`, `position`, `account`, `member`, `reward`, `leaderboard`다.

### Data And Integration Shape

- 운영 DB의 기본값은 MySQL이고, 테스트 DB는 H2다.
- DB 변경은 Flyway migration과 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 갱신한다.
- 영속성 기본 스택은 Spring Data JPA와 QueryDSL이다.
- 시장 데이터 외부 연동은 provider/connector 경계 뒤에 둔다.
- 로컬/분산 캐시는 기능 코드에 직접 흩뿌리지 않고 Spring Cache/Redis 기준으로 다룬다.

### Architecture Verification

백엔드는 아키텍처를 문서로만 유지하지 않고 `architectureLint`와 테스트로 일부 고정한다.

- 실행 위치: `backend/`
- 아키텍처 린트: `./gradlew architectureLint`
- 통합 검증: `./gradlew check`
- 리포트: `backend/build/reports/architecture-lint/violations.jsonl`

린트는 패키지 루트, 허용 top-level package, feature layer, domain framework-free, application service 간 직접 의존 금지, api의 persistence 직접 의존 금지 같은 규칙을 확인한다.

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
- 서버 컴포넌트의 초기 데이터 준비와 클라이언트 컴포넌트의 실시간 상호작용 연결

프론트 내부의 더 구체적인 codemap과 진입점은 [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)에 둔다.

### `backend/`

Spring Boot 기반 서비스다.

- 장기적으로 도메인 규칙의 최종 출처
- 영속성과 외부 연동의 중심
- 인증과 데이터 일관성 보장의 기준점
- 코인 선물 시장, 주문, 포지션, 계정, 회원, 보상 feature의 유스케이스와 저장소 경계

현재는 프론트보다 작은 상태지만, 구조적으로는 "보조 모듈"이 아니라 시스템의 진실한 규칙이 모여야 하는 자리다.

### `docs/`

런타임 바깥의 기억 장치다.

- `docs/product-specs/`: 제품 요구사항과 기능 사양
- `docs/design-docs/`: 백엔드/UI 설계 기준
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

### Backend Feature Layers

백엔드 내부에서는 기능별 수직 분리와 고정 레이어를 동시에 유지한다.

- 기능 경계: `feature/market`, `feature/order`, `feature/position`, `feature/account`, `feature/member`, `feature/reward`, `feature/leaderboard`
- 진입 경계: `api`
- 유스케이스 경계: `application`
- 비즈니스 의미와 상태 전이: `domain`
- DB, 외부 API, Spring config: `infrastructure`

세부 규칙은 루트 문서보다 백엔드 상세 설계 문서가 원문이다.

## Architectural Invariants

- 현재 사용자 경험의 진입점은 루트가 아니라 `frontend/`다.
- URL 수준에서 판단 가능한 접근 제어는 가능한 한 요청 경계에서 먼저 처리한다.
- 프론트에 임시 규칙이 있더라도, 영구적인 비즈니스 규칙은 백엔드로 수렴해야 한다.
- 문서는 구조와 의도를 설명하되, 자주 바뀌는 구현 디테일을 과하게 복제하지 않는다.
- 백엔드 feature 코드는 `feature/<name>/{api,application,domain,infrastructure}` 형태를 벗어나지 않는다.
- 도메인 레이어는 Spring/JPA/HTTP/외부 SDK 세부사항을 모른다.
- 교차 관심사는 개별 feature에 직접 흩어지지 않고 `Providers` 경계를 우선 통과한다.

## Known Transitional Areas

현재 코드가 최종 목표와 완전히 같은 상태는 아니다.
아래 영역은 작업할 때 특히 현재 현실과 목표 방향을 함께 확인해야 한다.

- 프론트에는 `stock` 라우트와 컴포넌트가 남아 있으며, `markets` 중심 흐름으로 점진 전환 중이다.
- 프론트에는 지원 심볼과 fallback market snapshot 같은 임시 제품 규칙이 남아 있다. 영구적인 비즈니스 규칙은 백엔드로 수렴시키는 것이 목표다.
- `frontend/components/router/`는 레거시 구조가 넓게 남아 있는 영역이다. 새 구조를 강제로 일부만 섞기보다, 관련 기능 단위로 정리한다.
- 백엔드 상세 규칙은 루트 문서가 아니라 `docs/design-docs/backend-design/` 번호 문서가 원문이다.

## Where To Start

- 저장소 실행과 전체 맥락: [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
- 설계 문서 구조와 배치 기준: [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
- 배포와 릴리즈 운영 기준: [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)
- 프론트 구조와 실제 진입점: [frontend/README.md](/Users/hj.park/projects/coin-zzickmock/frontend/README.md)
- 백엔드 작업 기준: [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- 백엔드 상세 설계: [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- 제품 배경과 구현 맥락: [docs/](/Users/hj.park/projects/coin-zzickmock/docs)

## Out of Scope

이 문서는 각 화면의 UX 세부사항, API 스키마, 스타일 규칙, 개별 훅과 컴포넌트의 동작까지 설명하지 않는다. 그런 내용은 더 가까운 문서나 코드 옆 문서가 맡는다.
