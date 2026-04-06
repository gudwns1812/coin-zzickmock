# stock-zzickmock

주식 서비스 실험을 위한 풀스택 프로젝트입니다.
프론트엔드는 Next.js 기반이고, 백엔드는 Spring Boot 기반으로 구성되어 있습니다.

## 기술 스택

- Frontend: Next.js 15, React 19, TypeScript
- Backend: Spring Boot 3, Java 17, Spring Data JPA, Redis, Spring Security
- Infra/Etc: MySQL, H2(Test), Gradle, npm workspace

## 프로젝트 구조

```text
stock-zzickmock/
├── frontend/                 # Next.js 프론트엔드
├── backend/                  # Spring Boot 백엔드
│   └── docs/
│       └── BACKEND_GUIDELINE.md
├── .codex/
│   └── skills/
│       └── backend-guideline-first/
├── Legacy.md                 # 리팩토링 분석 문서
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

## 백엔드 개발 규칙

이 프로젝트의 백엔드 작업은 먼저 가이드 문서를 확인하고 진행합니다.

- 가이드 문서: [backend/docs/BACKEND_GUIDELINE.md](/Users/hj.park/projects/stock-zzickmock/backend/docs/BACKEND_GUIDELINE.md)
- 로컬 스킬: [.codex/skills/backend-guideline-first/SKILL.md](/Users/hj.park/projects/stock-zzickmock/.codex/skills/backend-guideline-first/SKILL.md)

핵심 원칙:

- `Service`는 얇게 유지하고 세부 책임은 협력 객체로 분리
- 예외는 `CoreException`과 `ErrorType`으로 통일
- 응답은 `ApiResponse`로 통일
- 외부 연동은 `extern`, 스케줄링은 `batch`로 분리
- 리팩토링 시 기능 변경 없이 회귀 테스트 통과를 기준으로 검증

## 참고 문서

- 리팩토링 분석: [Legacy.md](/Users/hj.park/projects/stock-zzickmock/Legacy.md)
- 백엔드 개발 규율: [backend/docs/BACKEND_GUIDELINE.md](/Users/hj.park/projects/stock-zzickmock/backend/docs/BACKEND_GUIDELINE.md)

## 현재 상태

- 프론트엔드: npm workspace 기반으로 실행
- 백엔드: Gradle 기반 단독 실행
- 백엔드 구조는 객체지향 리팩토링과 응답/예외 통일화를 진행 중이며, 가이드 문서를 기준으로 계속 정리합니다.
