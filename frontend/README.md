# frontend

`coin-zzickmock`의 현재 사용자 경험을 담당하는 Next.js 워크스페이스입니다.

## Production

- Website: [https://coin-zzickmock-frontend.vercel.app](https://coin-zzickmock-frontend.vercel.app)
- Backend API: [https://coin-zzickmock.duckdns.org](https://coin-zzickmock.duckdns.org)
- Vercel project: `coin-zzickmock-frontend`
- Framework preset: `nextjs` via `vercel.json`

## Stack

- Next.js 15
- React 19
- TypeScript
- Tailwind CSS 4
- React Query
- Zustand

## Role

이 워크스페이스는 다음 책임을 가집니다.

- 라우트와 화면 레이아웃 조립
- 인증/접근 제어의 1차 처리
- 코인 선물 마켓, 포지션 계정, 관심 심볼, 상점 흐름 렌더링
- 백엔드 또는 프록시 호출 결과를 사용자에게 표시

## Routes

- `/markets`
- `/markets/[symbol]`
- `/portfolio`
- `/mypage`
- `/mypage/assets`
- `/mypage/points`
- `/mypage/redemptions`
- `/watchlist`
- `/community`
- `/community/[postId]`
- `/community/write`
- `/community/[postId]/edit`
- `/shop`
- `/admin`
- `/admin/reward-redemptions`
- `/admin/shop-items`
- `/login`
- `/signup`
- `/only-desktop`

## Key Entry Points

- `app/layout.tsx`
  전역 앱 셸입니다. Query Client, MSW, Toast를 조립합니다.
- `app/page.tsx`
  루트 진입점입니다. 현재는 `/markets` 쪽 흐름으로 보냅니다.
- `app/(main)/layout.tsx`
  로그인 이후 주요 화면의 공통 레이아웃입니다.
- `app/(main)/*/layout.tsx`
  로그인 여부와 레거시 라우트 리다이렉트 같은 서버 렌더링 경계 규칙을 처리합니다.

## Codemap

### `app/`

라우트와 레이아웃을 둡니다. "주소와 화면 흐름"이 이 폴더의 기준입니다.

- `app/(main)/markets/`: 마켓 목록/상세 흐름
- `app/(main)/portfolio/`: `/mypage`로 보내는 호환성 리다이렉트
- `app/(main)/mypage/`: 계정 정보, 자산, 포인트, 교환 내역 흐름
- `app/(main)/watchlist/`: 관심 심볼 흐름
- `app/(main)/community/`: 커뮤니티 목록과 게시글 읽기 흐름
- `app/(main)/shop/`: 포인트 상점 흐름
- `app/(main)/admin/`: 관리자 허브, 상점 아이템, 교환권 처리 흐름
- `app/login/`: 로그인 페이지
- `app/signup/`: 회원가입 흐름
- `app/only-desktop/`: 모바일 차단 화면
- `app/error.tsx`, `app/not-found.tsx`: 전역 오류/404 경계

페이지 파일은 가능한 한 얇게 유지하고, 반복되는 로직은 컴포넌트/훅/API 계층으로 내립니다.

### `components/`

렌더링 가능한 UI를 둡니다.

- `components/router/`
  라우트 조립에 가까운 컴포넌트와 provider를 둡니다.
- `components/ui/`
  화면에서 반복 사용하는 UI 조각을 둡니다.
- `components/ui/shared/`
  버튼, 입력, 에러 표시처럼 공통 조합을 둡니다.
- `components/animate-ui/`, `components/lottie/`
  표현 계층의 애니메이션과 시각 효과를 둡니다.

새 컴포넌트를 추가할 때는 "라우트 조립인지, 화면 조각인지, 공용 조합인지"를 먼저 구분합니다.

### `api/`

백엔드 또는 프록시 호출을 설명하는 얇은 함수 레이어입니다.

- 코인 선물 API 호출의 공통 경계는 `lib/futures-api.ts`와 `lib/futures-client-api.ts`입니다.
- 새 네트워크 함수는 `markets`, `account`, `orders`, `positions`, `watchlist`, `shop` 같은 코인 선물 도메인 이름을 사용합니다.

이 레이어는 네트워크 요청을 설명하는 역할에 집중하고, UI 토글이나 렌더링 관심사는 섞지 않는 편이 좋습니다.

### `hooks/`

브라우저에서 반복되는 상호작용과 클라이언트 동작을 캡슐화합니다.

- `useDebounce`, `useOutsideClick`: UI 상호작용 보조
- `useResilientEventSource`: SSE 재연결과 탭 복귀 흐름
- `useSessionActivityRefresh`: 사용자 활동 기반의 조용한 로그인 refresh

### `store/`

Zustand 기반 공유 상태를 둡니다.

- 현재 선물 서버 상태는 React Query와 서버 재조회 경계를 우선 사용합니다.
- 여러 화면이 공유해야 하는 순수 UI 상태만 store로 승격합니다.

서버에서 다시 가져올 수 있는 값은 무조건 store에 복제하기보다, 서버 상태 관리 도구를 우선 고려합니다.

### `utils/`, `lib/`, `type/`

- `utils/auth.ts`: 쿠키/JWT 기반 인증 정보 읽기 경계
- `utils/formatDate.ts`: 표현용 유틸
- `lib/`: 작은 공용 유틸과 시장 시드 데이터
- `type/`: API 응답과 도메인 타입

가능한 한 UI와 분리된 순수 코드로 유지하는 편이 좋습니다.

### `mocks/`

MSW 기반 목킹 코드를 둡니다. 백엔드와 프론트 작업을 분리하거나 불안정한 API를 우회할 때 사용합니다.

## Cross-cutting Concerns

### Authentication

인증 정보 읽기의 기준점은 `utils/auth.ts`입니다. 서버 컴포넌트 레이아웃과 보호 페이지가 이 경계를 통해 사용자 정보를 판단합니다.

### Query and App Providers

전역 provider 조립은 `app/layout.tsx`에서 끝냅니다. 새로운 cross-cutting concern을 추가할 때도 먼저 여기서 조립해야 하는지 검토합니다.

### Mocking

MSW는 `app/MSWProvider.tsx`와 `mocks/`를 중심으로 동작합니다.

### Coin Futures Boundary

- 새 메인 흐름은 `markets`, `mypage`, `watchlist`, `shop`입니다.
- `/portfolio`는 기존 북마크와 진입 링크를 위한 `/mypage` 호환 리다이렉트입니다.
- `frontend/lib/markets.ts`가 현재 지원 심볼과 기본 시드 데이터를 정의한다.

## Commands

루트에서 실행:

```bash
npm install
npm run dev
npm run build
```

워크스페이스만 직접 실행:

```bash
npm run dev --workspace frontend
npm run build --workspace frontend
```

## Where To Start

- 새 페이지/화면 흐름: `app/`
- 기존 화면 조립 변경: `app/(main)/layout.tsx`, 관련 route 파일
- 공용 UI 추가/수정: `components/ui/`, `components/ui/shared/`
- API 호출 추가: `api/`
- 반복 상호작용 추가: `hooks/`
- 여러 화면이 공유하는 상태 추가: `store/`
- 인증/접근 경계 변경: 보호 라우트의 `layout.tsx`/`page.tsx`, `utils/auth.ts`
- 지원 심볼/시장 시드 데이터 변경: `lib/markets.ts`

## Environment Variables

- `FUTURES_API_BASE_URL`: server-only backend base URL. 로컬 기본값은 `http://127.0.0.1:8080`, Vercel production 값은 `https://coin-zzickmock.duckdns.org`
- `NEXT_PUBLIC_API_MOCKING=enabled`: 선택, MSW 사용 시

Frontend Vercel 운영 환경 변수와 Preview/Production 배포 기준은
[docs/release-docs/05-frontend-vercel-operations.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/05-frontend-vercel-operations.md)를 따른다.

Tracked environment defaults:

- `.env.development`: local non-secret defaults
- `.env.test`: deterministic test defaults
- `.env.example`: local override template
- `.env.preview.example`: Vercel Preview scope template
- `.env.production.example`: Vercel Production scope template

Untracked local overrides:

- `.env.local`
- `.env.development.local`
- `.env.production.local`
- `.env.test.local`
