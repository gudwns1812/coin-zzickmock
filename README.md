# coin-zzickmock

코인 선물 모의 투자 플랫폼입니다.
프론트엔드는 Next.js 기반이고, 백엔드는 Spring Boot 기반으로 구성되어 있습니다.

## 기술 스택

- Frontend: Next.js 15, React 19, TypeScript
- Backend: Spring Boot 3, Java 17, Spring Data JPA, Redis, Spring Security
- Infra/Etc: MySQL, H2(Test), Gradle, npm workspace

## 프로젝트 구조

```text
coin-zzickmock/
├── frontend/                 # Next.js 프론트엔드
├── backend/                  # Spring Boot 백엔드
├── DESIGN.md                 # 설계 문서 배치 기준
├── FRONTEND.md               # 프론트 작업 기준 문서
├── BACKEND.md                # 백엔드 아키텍처 기준 문서
└── README.md
```

## 실행 방법

### 프론트엔드

루트에서 실행:

```bash
npm install
npm run dev
```

주요 명령어:

```bash
npm run build
npm run start
npm run lint
```

### 백엔드

`backend` 디렉터리에서 실행:

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew clean test
```

## 작업 기준

이 프로젝트의 주요 작업 기준 문서는 아래와 같습니다.

- 프론트 기준 문서: [FRONTEND.md](FRONTEND.md)
- 백엔드 기준 문서: [BACKEND.md](BACKEND.md)
- 배포/릴리즈 기준 문서: [RELEASE.md](RELEASE.md)
- 설계 틀: [DESIGN.md](DESIGN.md)
- 백엔드 상세 설계: [docs/design-docs/backend-design/README.md](docs/design-docs/backend-design/README.md)
- UI 상세 설계: [docs/design-docs/ui-design/README.md](docs/design-docs/ui-design/README.md)

## 백엔드 아키텍처 기준

이 프로젝트의 백엔드 작업은 `BACKEND.md`를 기준으로 진행합니다.

- 기준 문서: [BACKEND.md](BACKEND.md)
- 설계 틀: [DESIGN.md](DESIGN.md)
- 상세 설계: [docs/design-docs/backend-design/README.md](docs/design-docs/backend-design/README.md)

핵심 원칙:

- 기능은 `feature-first`로 자른다
- 레이어는 `api`, `application`, `domain`, `infrastructure`로 고정한다
- 교차 관심사(인증, 커넥터, 텔레메트리, 기능 플래그)는 `Providers`를 통해서만 접근한다
- 로컬 캐시는 `ConcurrentHashMap` 같은 ad-hoc 구조 대신 Spring Cache 뒤로 숨긴다
- 분산 캐시는 Redis를 표준 구현으로 사용하고, 기능 코드는 Redis client 세부사항보다 Spring Cache 경계를 먼저 의존한다
- 예외는 `CoreException`과 `ErrorType`으로 통일
- 응답은 `ApiResponse`로 통일
- 리팩토링 시 기능 변경 없이 회귀 테스트 통과를 기준으로 검증

## 참고 문서

- 백엔드 아키텍처 기준: [BACKEND.md](BACKEND.md)
- 프론트 작업 기준: [FRONTEND.md](FRONTEND.md)
- 배포/릴리즈 상세 문서: [docs/release-docs/README.md](docs/release-docs/README.md)
- 상세 설계 루트: [docs/design-docs/README.md](docs/design-docs/README.md)

## 현재 상태

- 프론트엔드: npm workspace 기반으로 실행
- 백엔드: Gradle 기반 단독 실행
- 백엔드 구조는 객체지향 리팩토링과 응답/예외 통일화를 진행 중이며, `BACKEND.md` 기준으로 계속 정리합니다.
