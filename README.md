# coin-zzickmock

`coin-zzickmock`는 Bitget 선물 시장 데이터를 바탕으로 사용자가 가상 USDT로 코인 선물 포지션, 레버리지, 마진, 청산가, 손익, 포인트 상점을 경험하는 데스크톱 우선 모의투자 플랫폼입니다.

실제 자산 수탁, 실주문 전송, 현금 입출금은 범위에 포함하지 않습니다. 시장 데이터는 실제 거래소 데이터를 사용할 수 있지만 주문 체결과 손익 반영은 서비스의 시뮬레이션 규칙을 따릅니다.

- Website: [https://coin-zzickmock-frontend.vercel.app](https://coin-zzickmock-frontend.vercel.app)
- Backend API: [https://coin-zzickmock.duckdns.org](https://coin-zzickmock.duckdns.org)

## 프로젝트 기능

- 회원가입/로그인과 계정별 초기 가상 잔고 `100000 USDT`
- `BTCUSDT`, `ETHUSDT` 중심의 코인 선물 마켓 목록과 심볼 상세
- 실시간 가격/SSE, 캔들, 시장 지표, 관심 심볼
- 시장가/지정가 주문, 롱/숏 포지션, 격리/교차 마진, 최대 `50x` 레버리지
- 주문 미리보기, 미체결 주문 수정/취소, 주문/체결/포지션/지갑 히스토리
- 포지션 TP/SL 조건부 종료 주문
- 마이페이지, 자산/포인트/교환 내역, 포인트 상점
- 관리자 상점 아이템과 리워드 교환권 처리
- 읽기 전용 수익률 리더보드와 운영 관측성 스택

## 기술 스택

- Frontend: Next.js 15, React 19, TypeScript, Tailwind CSS 4, React Query, Zustand, MSW
- Backend: Spring Boot 3.5, Java 17, Spring Data JPA, QueryDSL, Flyway, Spring Cache, Redis, MySQL, H2 tests, Actuator,
  Micrometer
- Infra/Operations: Docker Compose, Nginx, Prometheus, Grafana, Loki, Promtail, GitHub Actions CI/CD, Docker Hub, EC2
  SSH deploy
- Workspace: npm workspace for `frontend/`, Gradle wrapper for `backend/`

## 프로젝트 구조

```text
coin-zzickmock/
├── frontend/                  # 현재 사용자 경험을 담당하는 Next.js 앱
├── backend/                   # Spring Boot API/SSE 서비스와 도메인 규칙
├── docs/
│   ├── product-specs/         # 제품 동작, 사용자 흐름, 계산 규칙
│   ├── design-docs/           # 백엔드/UI 설계 기준
│   ├── exec-plans/            # 구현 계획과 완료 기록
│   ├── release-docs/          # 릴리즈, 롤아웃, 롤백, 관측성 운영 문서
│   └── generated/             # 현재 DB schema 같은 생성 산출물
├── infra/                     # Nginx, Prometheus, Grafana, Loki, Promtail 설정
├── docker-compose.yml         # 로컬 backend + DB/cache + 관측성 스택
├── docker-compose.prod.yml    # 운영 backend compose 계약
├── FRONTEND.md                # 프론트 작업 기준 문서
├── BACKEND.md                 # 백엔드 작업 기준 문서
├── RELEASE.md                 # 배포/릴리즈 기준 문서
├── OBSERVABILITY.md           # 메트릭/로그/모니터링 기준 문서
├── ARCHITECTURE.md            # 저장소 구조와 책임 경계
└── README.md
```

## 빠른 실행

### 프론트엔드

루트에서 실행합니다.

```bash
npm install
npm run dev
```

- 기본 개발 서버: `http://localhost:3000`
- 프론트 운영 배포는 Vercel이 담당하며 Docker Compose/CD 스택에는 포함하지 않습니다.
- 백엔드 API를 목킹해야 하면 `NEXT_PUBLIC_API_MOCKING=enabled`를 사용합니다.

주요 명령어:

```bash
npm run build
npm run start
npm run lint
npm test --workspace frontend
```

### 로컬 백엔드 전체 스택

루트에서 백엔드, MySQL, Redis, Nginx, Prometheus, Grafana, Loki를 함께 실행합니다.

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up --build -d
```

정리:

```bash
docker compose down
```

주요 URL:

- Backend health via Nginx: `http://localhost/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana via Nginx: `http://localhost/grafana/` (`admin` / `admin`)
- Grafana direct local port: `http://localhost:3001/grafana/`
- Loki API: `http://localhost:3100`

자세한 기준은 [docs/release-docs/observability/local-infra-stack.md](docs/release-docs/observability/local-infra-stack.md)를
참고합니다.

### 백엔드 단독 실행

백엔드를 직접 띄울 때는 먼저 로컬 MySQL/Redis가 필요합니다. Compose로 의존성만 띄운 뒤 Gradle로 실행할 수 있습니다.

```bash
docker compose up -d mysql redis
cd backend
./gradlew bootRun
```

- 직접 실행 시 기본 backend URL: `http://localhost:8080`
- 기본 DB: `jdbc:mysql://localhost:3306/coin_zzickmock`
- 기본 Redis: `localhost:6379`
- 기본 JWT secret과 로컬 DB 비밀번호는 `backend/src/main/resources/application.yml`의 개발용 기본값을 사용합니다.

## 주요 라우트와 API 경계

프론트 주요 라우트:

- `/markets`, `/markets/[symbol]`
- `/portfolio`
- `/mypage`, `/mypage/assets`, `/mypage/points`, `/mypage/redemptions`
- `/watchlist`
- `/shop`
- `/admin`, `/admin/reward-redemptions`, `/admin/shop-items`
- `/login`, `/signup`, `/only-desktop`

백엔드 HTTP/SSE 경계는 `/api/futures/**` 아래에 있습니다.

- Auth: `/api/futures/auth/**`
- Markets: `/api/futures/markets/**`
- Account: `/api/futures/account/**`
- Orders: `/api/futures/orders/**`
- Positions: `/api/futures/positions/**`
- Rewards/Shop/Admin: `/api/futures/rewards/**`, `/api/futures/shop/**`, `/api/futures/admin/**`
- Leaderboard: `/api/futures/leaderboard`

## 검증 명령

프론트:

```bash
npm run lint
npm run build
npm test --workspace frontend
```

백엔드:

```bash
cd backend
./gradlew architectureLint --console=plain
./gradlew check --console=plain
```

CI는 브랜치명 정책, 프론트 typecheck/build, 백엔드 `./gradlew check`를 검증합니다.

## 배포와 운영

- CI: [.github/workflows/ci.yml](.github/workflows/ci.yml)
- Frontend production: [https://coin-zzickmock-frontend.vercel.app](https://coin-zzickmock-frontend.vercel.app)
- Frontend Vercel 운영: [docs/release-docs/05-frontend-vercel-operations.md](docs/release-docs/05-frontend-vercel-operations.md)
- Backend CD: [.github/workflows/cd.yml](.github/workflows/cd.yml)
- 운영 compose 계약: [docker-compose.prod.yml](docker-compose.prod.yml)
- 운영 환경/산출물 기준: [docs/release-docs/01-environments-and-artifacts.md](docs/release-docs/01-environments-and-artifacts.md)
- Backend production CD 기준: [docs/release-docs/04-production-cd.md](docs/release-docs/04-production-cd.md)

현재 CD는 `main`/`master`의 backend, production compose, infra 변경 또는 수동 실행을 배포 효과로 분류합니다. Backend 코드 변경은 backend 릴리즈 후보를
검증하고 Docker Hub에 ARM64 backend 이미지를 발행한 뒤 EC2 `.env.prod`의 `BACKEND_IMAGE`만 새 태그로 바꿔 backend를 pull/restart합니다.
Infra/compose 변경은 backend image를 새로 만들지 않고, 분류된 runtime 범위에 따라 backend 또는 non-backend 서비스를 재시작하거나 Nginx 설정만 reload합니다.

## 작업 기준 문서

작업을 시작할 때는 변경 영역에 맞는 가장 가까운 입구 문서를 먼저 읽습니다.

- 저장소 구조와 책임 경계: [ARCHITECTURE.md](ARCHITECTURE.md)
- 프론트 작업 기준: [FRONTEND.md](FRONTEND.md), [frontend/README.md](frontend/README.md)
- 백엔드 작업 기준: [BACKEND.md](BACKEND.md)
- 제품 명세: [docs/product-specs/README.md](docs/product-specs/README.md)
- 백엔드 상세 설계: [docs/design-docs/backend-design/README.md](docs/design-docs/backend-design/README.md)
- UI 상세 설계: [docs/design-docs/ui-design/README.md](docs/design-docs/ui-design/README.md)
- 배포/릴리즈 기준: [RELEASE.md](RELEASE.md), [docs/release-docs/README.md](docs/release-docs/README.md)
- 관측성 기준: [OBSERVABILITY.md](OBSERVABILITY.md)
- DB schema 기준: [docs/generated/db-schema.md](docs/generated/db-schema.md)
- 브랜치/PR 규칙: [docs/process/branch-and-pr-rules.md](docs/process/branch-and-pr-rules.md)

사용자 동작, API 계약, DB schema, 배포 계약이 바뀌는 작업은 코드와 함께 관련 문서도 갱신합니다.

## 아키텍처 기준

프론트:

- 기본값은 Server Component입니다.
- 브라우저 API, 사용자 입력, DOM 제어, client-only state가 필요할 때만 Client Component를 사용합니다.
- 새 server state는 React Query를 사용하고, 서버에서 다시 가져올 수 있는 상태를 Zustand에 새로 얹지 않습니다.
- `response.ok` 확인 없이 `res.json()`을 호출하지 않습니다.
- 관련 없는 작업에서 `src/`나 `features/` 구조를 부분 도입하지 않습니다.

백엔드:

- 기능은 `feature/<feature-name>` 아래에서 수직으로 자릅니다.
- 레이어는 `web`, `job`, `application`, `domain`, `infrastructure`로 고정합니다.
- 도메인 모델은 Spring/JPA/HTTP/외부 SDK 세부사항을 모릅니다.
- 인증, 커넥터, 텔레메트리, 기능 플래그 같은 교차 관심사는 `providers/Providers.java` 경계 뒤로 모읍니다.
- DB 변경은 새 Flyway migration과 [docs/generated/db-schema.md](docs/generated/db-schema.md) 갱신을 함께 수행합니다.
- 비즈니스 실패는 `CoreException`과 구조화된 error type으로 표현합니다.

## 브랜치 네이밍

브랜치명은 반드시 `<type>/<kebab-case-summary>` 형식이어야 합니다.
허용 type은 `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`, `perf`, `style`, `build`, `revert`입니다.

`codex/*`, `codex-*`, `[codex]` 같은 자동화 접두사는 금지하며 외부 도구 기본값보다 이 저장소 규칙이 우선합니다.

```bash
npm run check:branch -- feat/example-branch
```
