# 05. Frontend Vercel Operations

## Purpose

이 문서는 `frontend/` Next.js 앱을 Vercel에서 운영 배포하기 위한 기준을 정의한다.
목표는 Vercel Git Integration을 기본 배포 경로로 쓰면서, 현재 GitHub CI와 backend EC2 운영 배포를 서로 충돌 없이 연결하는 것이다.

공식 참고:

- [Vercel Git deployments](https://vercel.com/docs/deployments/git)
- [Vercel monorepos](https://vercel.com/docs/monorepos)
- [Vercel rollback CLI](https://vercel.com/docs/cli/rollback)

## Deployment Model

프론트엔드는 Vercel에서 배포한다.
백엔드 운영 API는 `https://coin-zzickmock.duckdns.org`를 기준으로 연결한다.

기본 전략:

- Vercel Git Integration을 사용한다.
- Production branch는 `main`을 기준으로 둔다.
- Pull Request와 production branch가 아닌 branch는 Preview Deployment로 확인한다.
- GitHub Actions의 `Frontend Typecheck And Build`와 `Backend Architecture Lint And Test`를 병합 전 품질 게이트로 유지한다.
- 운영 배포는 리뷰되지 않은 로컬 상태나 미병합 브랜치가 아니라, CI를 통과한 commit SHA 기준으로 식별한다.

GitHub Actions에서 `vercel deploy --prebuilt`를 직접 실행하는 커스텀 CD는 아직 기본값으로 두지 않는다.
현재 저장소는 Vercel Git Integration과 기존 CI 조합이 더 단순하고, Vercel이 PR Preview와 production branch 배포를 자동으로 제공하기 때문이다.
커스텀 CD는 E2E 테스트 후 `vercel promote`가 필요한 단계가 생겼을 때 별도 설계한다.

## Vercel Project Settings

Vercel project는 GitHub repository에서 import한다.

권장 설정:

| Setting | Value |
| --- | --- |
| Framework Preset | `Next.js` |
| Root Directory | `frontend` |
| Install Command | Vercel 기본값 |
| Build Command | `npm run build` |
| Output Directory | Vercel 기본값 |
| Production Branch | `main` |
| Node.js Version | `24.x` |

이 저장소는 루트 `package.json`의 npm workspace와 루트 `package-lock.json`을 사용한다.
Vercel monorepo 설정은 repository root의 lockfile과 workspace 정보를 기준으로 동작해야 하므로, project 연결과 CLI 링크 작업은 repository root 기준으로 수행한다.
Node.js runtime은 Vercel의 현재 기본 LTS인 `24.x`와 GitHub Actions CI를 맞춘다.
`20.x`는 2026-04-30에 End-of-Life가 되었으므로 새 배포 기준으로 쓰지 않는다.

`vercel.json`은 지금 단계에서 필수로 추가하지 않는다.
Root Directory, build command, environment variables, production branch는 Vercel Project Settings를 원본으로 둔다.
저장소에 versioned 설정이 필요해지는 시점에만 `frontend/vercel.json` 또는 project root 설정 파일 도입을 별도 변경으로 검토한다.

## Environment Variables

Vercel에는 환경별로 값을 나눠 둔다.
실제 secret 원문은 문서와 Git에 넣지 않는다.
Next.js 환경 파일은 `frontend/`를 project root로 보고 그 아래에 둔다.

Tracked defaults and templates:

| File | Purpose |
| --- | --- |
| `frontend/.env.development` | 로컬 개발용 비밀 없는 기본값 |
| `frontend/.env.test` | 테스트용 결정적 기본값 |
| `frontend/.env.example` | `.env.local` 작성 시작점 |
| `frontend/.env.preview.example` | Vercel Preview scope에 넣을 값의 템플릿 |
| `frontend/.env.production.example` | Vercel Production scope에 넣을 값의 템플릿 |

Untracked local override files:

- `frontend/.env.local`
- `frontend/.env.development.local`
- `frontend/.env.production.local`
- `frontend/.env.test.local`

Next.js는 이미 존재하는 `process.env` 값을 가장 먼저 사용한다.
따라서 Vercel Production/Preview에서는 dashboard 또는 `vercel env`로 주입한 값이 committed `.env.*` 파일보다 우선한다.

### Production

| Name | Value | Visibility | Purpose |
| --- | --- | --- | --- |
| `FUTURES_API_BASE_URL` | `https://coin-zzickmock.duckdns.org` | Server-only | Next.js rewrite, route handler, server fetch가 호출할 backend base URL |
| `NEXT_PUBLIC_API_MOCKING` | unset | Public | production에서는 MSW를 켜지 않는다 |
| `NEXT_PUBLIC_BASE_URL` | 운영 legacy stock API base URL, 필요한 경우만 | Public | 남아 있는 legacy stock route용 |
| `NEXT_PUBLIC_BASE_URL2` | 운영 legacy stock API base URL, 필요한 경우만 | Public | 남아 있는 legacy stock API 호출용 |
| `SENTRY_AUTH_TOKEN` | secret value, sourcemap upload 사용 시 | Server-only build secret | Sentry sourcemap upload |

강한 규칙:

- `FUTURES_API_BASE_URL`에는 `/api/futures`를 붙이지 않는다. `frontend/next.config.ts`와 route handler가 path를 붙인다.
- Vercel 배포에서 `FUTURES_API_BASE_URL`이 없으면 frontend build/runtime은 실패해야 한다. 조용히 localhost fallback으로 배포하지 않는다.
- `NEXT_PUBLIC_*`에는 공개 가능한 값만 둔다.
- production Vercel env에서 `NEXT_PUBLIC_API_MOCKING=enabled`를 설정하지 않는다.
- backend JWT signing secret은 backend runtime에만 둔다. 현재 frontend는 쿠키 토큰을 표시용으로 decode할 뿐 서명 검증을 하지 않는다.

### Preview

Preview 환경도 기본값은 production backend URL을 사용한다.

| Name | Value |
| --- | --- |
| `FUTURES_API_BASE_URL` | `https://coin-zzickmock.duckdns.org` |
| `NEXT_PUBLIC_API_MOCKING` | unset |

주의:

- Preview가 production backend를 호출하면 테스트 로그인, 주문, 포인트, 관리자 조작이 운영 데이터에 영향을 줄 수 있다.
- staging backend가 생기면 Preview의 `FUTURES_API_BASE_URL`을 staging backend URL로 바꾸고 이 문서를 갱신한다.
- 운영 데이터에 쓰기 영향을 줄 수 있는 Preview 검증은 테스트 계정과 낮은 위험 흐름으로 제한한다.

### Local

로컬 개발은 기존 기본값을 유지한다.

```bash
npm run dev --workspace frontend
```

로컬 secret이나 개인별 override가 필요하면 `frontend/.env.example`을 `frontend/.env.local`로 복사해 수정한다.
Vercel Development scope 값을 가져와 맞춰 보고 싶으면 repository root에서 project link를 잡은 뒤 아래처럼 가져온다.

```bash
vercel env pull frontend/.env.local --environment=development --yes
```

## Release Flow

### Pull Request Preview

1. `<type>/<kebab-case-summary>` 형식의 branch에서 PR을 연다.
2. GitHub Actions `CI`가 branch name, frontend build, backend check를 검증한다.
3. Vercel이 Preview Deployment를 생성한다.
4. PR에서 Preview URL로 핵심 화면을 확인한다.
5. CI와 Preview 확인이 끝난 뒤 `main`에 merge한다.

Preview smoke checks:

- `/login` 진입
- `/markets` 진입
- `/markets/[symbol]`에서 market stream 또는 candle stream이 깨지지 않는지 확인
- `/portfolio` 진입
- 주문 관련 UI가 production backend에 연결되는지 확인하되, 실제 운영 영향이 있는 write는 테스트 계정으로만 수행
- 브라우저 콘솔 치명 오류와 Vercel runtime error가 없는지 확인

### Production Deployment

1. `main` merge 후 Vercel Production Deployment가 생성되는지 확인한다.
2. 대상 deployment의 commit SHA가 merge commit 또는 의도한 release commit과 같은지 확인한다.
3. Production URL에서 smoke checks를 수행한다.
4. 릴리즈 기록에 Vercel deployment URL, commit SHA, 실행 시각, smoke 결과를 남긴다.
5. 배포 후 30분 동안 backend Grafana/Sentry/Vercel error 신호를 확인한다.

Production smoke checks:

- `/login` 또는 인증 진입 흐름
- `/markets`
- `/markets/[symbol]`
- `/portfolio`
- backend health 또는 공개 상태 점검 경로
- 치명적인 client console error, Vercel function error, backend 5xx 급증 여부

## Full-stack Rollout Order

프론트와 백엔드가 함께 바뀔 때는 [03-rollout-and-rollback.md](03-rollout-and-rollback.md)의 호환성 원칙을 따른다.

하위 호환 변경:

1. Backend EC2 CD 배포
2. Backend health와 핵심 API smoke 확인
3. Frontend Vercel Preview 확인
4. `main` merge 또는 Vercel production 배포
5. Frontend smoke 확인

breaking 가능성이 있는 변경:

- backend와 frontend를 동시에 기대고 배포하지 않는다.
- expand-and-contract, feature flag, 또는 2단계 배포 계획을 먼저 적는다.
- 롤백 시 frontend만 되돌렸을 때 backend 신버전과 호환되는지 확인한다.

## Rollback

프론트 장애가 production deployment와 연관되어 있으면 Vercel rollback을 우선 검토한다.

```bash
vercel rollback
```

특정 deployment로 되돌려야 하는 경우:

```bash
vercel rollback <deployment-url-or-id>
```

롤백 후:

- Production URL smoke checks를 다시 수행한다.
- backend 5xx, auth failure, market stream failure가 정상화됐는지 확인한다.
- 릴리즈 기록에 원인 가설, 롤백 deployment, 후속 조치를 남긴다.

롤백 대신 hotfix를 고르는 경우는 아래처럼 rollback이 더 위험할 때로 제한한다.

- backend schema/API가 이미 forward-only로 바뀌어 이전 frontend가 더 위험하다.
- 장애가 작은 copy/config 문제이고 hotfix가 더 빠르며 검증 범위가 명확하다.
- Vercel rollback 대상 deployment가 필요한 환경 변수 계약을 더 이상 만족하지 못한다.

## Observability

Frontend 운영 신호는 Vercel과 Sentry를 보조 신호로 사용하고, backend 상태는 기존 Grafana/Prometheus/Loki 기준을 따른다.

배포 후 확인 신호:

- Vercel deployment status가 `READY`인지
- Vercel runtime/function error가 급증하지 않는지
- Sentry client/server error가 급증하지 않는지
- backend Grafana에서 `auth`, `market_read`, `history_read`, `trading_write`, `sse_open`, `external_proxy` 그룹의 5xx와 p95/p99가 악화되지 않았는지
- backend logs에 Vercel proxy에서 유입된 실패가 반복되지 않는지

로그와 메트릭에는 [OBSERVABILITY.md](../../OBSERVABILITY.md)의 개인정보와 낮은 카디널리티 규칙을 적용한다.
Vercel 또는 Sentry에 token, cookie, raw request body, user identifier 원문을 남기지 않는다.

## Release Record Fields

프론트 Vercel 릴리즈 기록에는 최소 아래를 남긴다.

- 대상 environment: `preview` 또는 `production`
- Vercel deployment URL
- Vercel project name
- commit SHA
- production branch 또는 PR branch
- `FUTURES_API_BASE_URL` 값의 environment 이름과 실제 base URL
- GitHub Actions CI 결과
- smoke checks 결과
- rollback 기준 deployment
- 배포 후 관측 결과

## Maintenance Rule

- Vercel project setting이 바뀌면 이 문서를 갱신한다.
- frontend 환경 변수가 추가되면 이 문서와 [01-environments-and-artifacts.md](01-environments-and-artifacts.md)를 함께 갱신한다.
- staging backend가 생기면 Preview 환경의 backend URL과 smoke rule을 갱신한다.
- GitHub Actions에서 Vercel CLI 기반 custom CD를 도입하면 이 문서의 Deployment Model과 Release Flow를 먼저 바꾼다.
