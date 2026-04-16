# 코인 선물 MVP 기반 구축 계획

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서 하나만 읽어도 초보자가 코인 선물 모의투자 MVP의 기반 구현을 어디서부터 어떻게 시작해야 하는지 이해할 수 있게 작성한다.

## 목적 / 큰 그림

이 작업의 목적은 현재 주식 조회 중심 프론트를 코인 선물 모의투자 제품으로 전환할 수 있는 최소 동작 기반을 만드는 것이다.
완료 후 사용자는 회원가입을 하고, `BTCUSDT` 또는 `ETHUSDT` 시장을 보고, 최대 `50x` 레버리지로 `ISOLATED` 또는 `CROSS` 마진 포지션을 열 수 있어야 한다.
또한 손익과 포인트 적립, 상점 소비 흐름의 뼈대가 연결되어 있어야 한다.

이 계획은 한 번에 모든 것을 끝내는 문서가 아니라, 큰 목표를 구현 가능한 작은 블록으로 나누어 순서와 검증 기준을 고정하는 문서다.

## 진행 현황

- [x] (2026-04-16 12:05+09:00) 이 계획은 stale/superseded 상태로 종료됨
- [x] (2026-04-16 09:44+09:00) 제품 설계 초안 작성 완료
- [x] (2026-04-16 10:05+09:00) 사용자 요구사항 반영 완료: Bitget, BTCUSDT/ETHUSDT, 회원가입, `100000 USDT`, `50x`, maker/taker 수수료, 포인트/상점
- [x] (2026-04-16 10:12+09:00) 화면 명세와 시뮬레이션 규칙 문서 분리 완료
- [x] (2026-04-16 10:12+09:00) 사용자 승인 완료
- [x] (2026-04-16 10:20+09:00) 프론트 라우트 전환 및 공통 타입 정리 완료
- [x] (2026-04-16 10:25+09:00) 백엔드 feature 골격 생성 완료
- [x] (2026-04-16 10:40+09:00) Bitget 시장 데이터 fallback 연결 완료
- [x] (2026-04-16 10:40+09:00) 주문/포지션 엔진 1차 구현 완료
- [x] (2026-04-16 10:40+09:00) 포인트/상점 1차 백엔드 API 구현 완료
- [x] (2026-04-16 10:53+09:00) 프론트 futures API 연결 완료
- [x] (2026-04-16 10:53+09:00) 프론트 빌드 통과
- [x] (2026-04-16 10:25+09:00) `./gradlew architectureLint` 통과
- [x] (2026-04-16 10:25+09:00) `./gradlew test` 통과
- [ ] 후속 리팩터링용 신규 계획 수립

## 놀라움과 발견

- 관찰:
  현재 백엔드는 [backend/src/main/java/coin/coinzzickmock/bootstrap/CoinZzickmockApplication.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/bootstrap/CoinZzickmockApplication.java)만 있는 사실상 빈 상태다.
  증거:
  `find backend/src/main/java -maxdepth 5 -type f | sort` 결과에 애플리케이션 엔트리포인트만 존재했다.

- 관찰:
  프론트는 레이아웃 자산은 재사용 가능하지만, `stocks`, `portfolio`, `useRealTimeStock`처럼 주식 의미가 깊게 박혀 있다.
  증거:
  [frontend/api/stocks.ts](/Users/hj.park/projects/coin-zzickmock/frontend/api/stocks.ts), [frontend/hooks/useRealTimeStock.ts](/Users/hj.park/projects/coin-zzickmock/frontend/hooks/useRealTimeStock.ts), [frontend/app/(main)/stock/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock/page.tsx)를 읽어 확인했다.

- 관찰:
  현재 `utils/auth.ts`는 JWT를 decode만 하고 있어 서버 최종 검증 기준으로는 불충분하다.
  증거:
  [frontend/utils/auth.ts](/Users/hj.park/projects/coin-zzickmock/frontend/utils/auth.ts)에 `jwtDecode`만 있고 verify는 주석 처리되어 있다.

- 관찰:
  `app/(main)/layout.tsx`에 있는 `ActiveStockRequestCoordinator`는 새 코인 선물 라우트와 무관한 주식 전용 교차 관심사였다.
  증거:
  컴포넌트가 `/stock` pathname과 `/proxy2/v2/stocks/active-sets`에 직접 의존하고 있었다. 새 라우트 전환 단계에서 제거해도 빌드가 통과했다.

- 관찰:
  백엔드 아키텍처 린트는 package shape만 맞추면 바로 강한 가드레일 역할을 한다.
  증거:
  `./gradlew architectureLint` 실행 결과 `violations: 0`으로 통과했고, 새 `providers`, `common`, `feature/*` 골격이 규칙을 만족했다.

- 관찰:
  Bitget market ticker 하나로 `last price`, `mark price`, `index price`, `funding rate`, `change24h`를 한 번에 채울 수 있다.
  증거:
  `GET /api/v2/mix/market/ticker` 응답 필드를 기준으로 fallback connector를 구현했고, 외부 호출 실패 시에도 시드 데이터로 동작하도록 만들었다.

- 관찰:
  프론트는 서버 컴포넌트 fetch와 클라이언트 액션 rewrite를 분리하는 편이 안전하다.
  증거:
  `frontend/lib/futures-api.ts`는 서버에서 `http://127.0.0.1:8080`을 직접 읽고 실패 시 시드 데이터로 fallback 하도록 만들었고, 클라이언트 주문/청산은 `frontend/next.config.ts`의 `/proxy-futures/*` rewrite를 통해 백엔드에 연결했다. 이 구조에서 `npm run build --workspace frontend`가 통과했다.

## 의사결정 기록

- 결정:
  시세 데이터 소스는 `Bitget`으로 고정한다.
  근거:
  제품 요구사항에서 명시되었고, MVP 심볼이 `BTCUSDT`, `ETHUSDT` 두 개뿐이라 특정 거래소 기준으로 단순화하는 것이 구현과 검증에 유리하다.
  날짜/작성자:
  2026-04-16 / Codex + 사용자 합의

- 결정:
  마진 모드는 `ISOLATED`, `CROSS`를 모두 MVP에 포함한다.
  근거:
  사용자가 둘 다 체험 가치가 크다고 판단했고, 선물 교육 목적에도 부합한다.
  날짜/작성자:
  2026-04-16 / Codex + 사용자 합의

- 결정:
  프론트는 기존 주식 코드를 덮어쓰지 않고, 새 도메인 이름으로 병행 생성 후 점진 전환한다.
  근거:
  기존 파일명이 도메인 오해를 유발하고, 백엔드도 새로 세워야 하므로 점진 전환이 회귀 위험이 낮다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  지정가 주문은 "최신 체결가가 지정가를 넘거나 같아지면 체결" 규칙으로 시작하고, 즉시 체결 가능한 지정가는 `taker`, 대기 후 체결은 `maker`로 본다.
  근거:
  사용자가 명시한 체결 직관을 그대로 살리면서도 구현 규칙을 단순하게 유지할 수 있다.
  날짜/작성자:
  2026-04-16 / Codex + 사용자 합의

- 결정:
  이 계획은 stale/superseded 상태로 종료하고 `completed`로 이동한다.
  근거:
  상위 백엔드 기준 문서에서 `port/usecase` 인터페이스를 기본값으로 두지 않는 방향으로 설계 기준이 바뀌었고, 현재 계획 문서의 구조 설명이 그 이전 방향을 담고 있어 더 이상 구현 기준으로 바로 사용할 수 없게 되었다.
  날짜/작성자:
  2026-04-16 / Codex + 사용자 지시

## 결과 및 회고

이 계획은 merge 완료 계획이 아니라, 기준 변경으로 인해 stale/superseded 상태가 된 계획 기록이다.
따라서 이 문서는 현재 구현 기준의 원문이 아니라 이전 MVP 기반 구축 맥락과 왜 새 계획이 필요한지 설명하는 종료 기록으로 읽어야 한다.

현재까지 프론트 첫 블록과 백엔드 핵심 API 블록을 실제 코드로 옮겼다.

- `/markets`, `/markets/[symbol]`, `/watchlist`, `/shop`, `/login` 라우트를 추가했다.
- 루트 진입점, 헤더, 푸터, 오류 화면, 미들웨어를 새 코인 선물 흐름으로 전환했다.
- `frontend/lib/markets.ts`에 지원 심볼과 마켓 시드 데이터를 고정했다.
- `frontend/lib/futures-api.ts`를 추가해 서버 렌더링에서 futures API를 읽고 실패 시 fallback 하도록 만들었다.
- 기존 `portfolio` 화면을 실제 계정/포지션/포인트 데이터를 읽는 선물 계정 대시보드로 교체했다.
- `frontend/components/futures/OrderEntryPanel.tsx`, `frontend/components/futures/ClosePositionButton.tsx`를 추가해 주문 미리보기, 주문 실행, 포지션 종료 액션을 연결했다.
- 백엔드에 `common/api`, `providers`, `feature/account|market|order|position|reward` 골격과 초기 계약 타입을 추가했다.
- 백엔드에 `GET /api/futures/markets`, `GET /api/futures/markets/{symbol}`, `GET /api/futures/account/me`, `GET /api/futures/positions/me`, `POST /api/futures/orders/preview`, `POST /api/futures/orders`, `POST /api/futures/positions/close`, `GET /api/futures/rewards/me`, `GET /api/futures/shop/items`를 추가했다.
- Bitget ticker fallback connector와 인메모리 account/order/position/reward 저장소를 추가했다.
- reward 포인트 구간에 대한 첫 단위 테스트를 추가했다.
- order 생성에 대한 첫 단위 테스트를 추가했다.
- `npm run build --workspace frontend`가 통과했다.
- `./gradlew architectureLint`, `./gradlew test`가 통과했다.

남은 큰 블록은 아래 2개다.

1. 인증과 계정 생성을 외부 프록시 의존 없이 로컬 백엔드 기준으로 정리
2. Bitget candles, 대기 주문 체결 루프, 구매 처리 API까지 확장

이제 다음 작업자는 화면 이름을 다시 정하는 데 시간을 쓰지 않고, 백엔드 feature 골격과 실제 데이터/계산 연결에 바로 들어가면 된다.

## 맥락과 길잡이

현재 저장소는 루트에 기준 문서가 있고, 런타임 코드는 `frontend/`와 `backend/`에 나뉘어 있다.

### 먼저 읽어야 할 문서

- [README.md](/Users/hj.park/projects/coin-zzickmock/README.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [SECURITY.md](/Users/hj.park/projects/coin-zzickmock/SECURITY.md)
- [docs/product-specs/coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)
- [docs/product-specs/coin-futures-screen-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-screen-spec.md)
- [docs/product-specs/coin-futures-simulation-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-simulation-rules.md)

### 현재 프론트 핵심 경로

- [frontend/app/(main)/layout.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/layout.tsx)
- [frontend/app/(main)/markets/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx)
- [frontend/app/(main)/markets/[symbol]/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/[symbol]/page.tsx)
- [frontend/app/(main)/portfolio/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/portfolio/page.tsx)
- [frontend/app/(main)/watchlist/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/watchlist/page.tsx)
- [frontend/app/(main)/shop/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/shop/page.tsx)
- [frontend/components/futures/OrderEntryPanel.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/futures/OrderEntryPanel.tsx)
- [frontend/components/futures/ClosePositionButton.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/futures/ClosePositionButton.tsx)
- [frontend/lib/futures-api.ts](/Users/hj.park/projects/coin-zzickmock/frontend/lib/futures-api.ts)
- [frontend/app/login/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/login/page.tsx)
- [frontend/lib/markets.ts](/Users/hj.park/projects/coin-zzickmock/frontend/lib/markets.ts)

### 현재 백엔드 상태

백엔드는 이제 최소 feature 골격이 생긴 상태다.
목표 패키지 구조는 [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)의 `feature-first` 구조를 따르며, 현재 아래 패키지들이 생성되어 있다.

예상 기능 패키지는 아래와 같다.

- `coin.coinzzickmock.feature.market`
- `coin.coinzzickmock.feature.account`
- `coin.coinzzickmock.feature.order`
- `coin.coinzzickmock.feature.position`
- `coin.coinzzickmock.feature.history`
- `coin.coinzzickmock.feature.reward`

### 선행 조건

- 루트에서 `npm install`
- `backend/`에서 Gradle 빌드 가능 상태 확인
- Bitget API 연결 방식 결정: polling 우선

## 작업 계획

### 블록 1. 프론트 도메인 전환

[frontend/app/(main)/stock](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/stock) 중심 구조를 `markets` 기준으로 옮기고, 주식 의미가 담긴 타입과 API 파일을 새 이름으로 분리한다.
이 단계에서는 UI를 완전히 새로 만드는 것이 아니라, 라우트와 데이터 모델 이름을 먼저 바로잡는다.

완료 결과:

- `frontend/app/(main)/markets/page.tsx`
- `frontend/app/(main)/markets/[symbol]/page.tsx`
- `frontend/app/(main)/watchlist/page.tsx`
- `frontend/app/(main)/shop/page.tsx`
- `frontend/app/login/page.tsx`
- `frontend/lib/markets.ts`
- `frontend/middleware.ts`
- `frontend/components/ui/shared/header/Header.tsx`
- `frontend/components/ui/shared/header/Navigation.tsx`
- `frontend/components/ui/shared/Footer.tsx`

### 블록 2. 인증과 계정 기초

회원가입과 로그인을 만들고, 가입 시 `100000 USDT`와 기본 watchlist를 지급한다.
프론트와 백엔드 모두 계정 개념을 맞춘다.

수정 대상 후보:

- `frontend/app/signup/*`
- `frontend/app/login/*`
- `backend/src/main/java/coin/coinzzickmock/feature/account/**`
- `backend/src/main/java/coin/coinzzickmock/feature/member/**`

현재 상태:

- `feature/account`의 query/result/usecase/domain/infrastructure 인터페이스 골격은 생성됨
- `GET /api/futures/account/me`는 동작함
- 실제 회원가입, 로그인, 계정 생성과 초기 잔고 지급을 로컬 백엔드 기준으로 붙이는 작업은 아직 없음

### 블록 3. Bitget 시장 데이터 연결

Bitget에서 `BTCUSDT`, `ETHUSDT` ticker, candles, funding 관련 데이터를 받아 프론트에 제공한다.
초기에는 polling과 Redis 캐시로 시작한다.

수정 대상 후보:

- `backend/.../feature/market/api/**`
- `backend/.../feature/market/application/**`
- `backend/.../feature/market/infrastructure/connector/**`
- `backend/.../providers/connector/**`
- `frontend/api/markets.ts`

현재 상태:

- `feature/market` 최소 결과 타입과 `LoadMarketSnapshotPort`는 생성됨
- `providers/connector/MarketDataGateway`는 생성됨
- `GET /api/futures/markets`, `GET /api/futures/markets/{symbol}`는 동작함
- Bitget ticker HTTP 연결 구현체와 실패 시 fallback 시드 데이터가 함께 존재함
- 프론트 `/markets`, `/markets/[symbol]`, `/watchlist`는 이 API를 실제로 읽도록 연결됨

### 블록 4. 주문과 포지션 엔진

시장가/지정가 주문, maker/taker 수수료, `ISOLATED`/`CROSS`, `50x`, 미실현/실현 손익, 청산가 계산을 구현한다.
이 단계가 MVP의 핵심이다.

수정 대상 후보:

- `backend/.../feature/order/**`
- `backend/.../feature/position/**`
- `backend/.../feature/history/**`
- `frontend/components/.../OrderEntryPanel.tsx`
- `frontend/components/.../PositionTable.tsx`

현재 상태:

- `POST /api/futures/orders/preview`, `POST /api/futures/orders`, `POST /api/futures/positions/close`는 동작함
- 프론트 `OrderEntryPanel`과 `ClosePositionButton`이 이 API에 연결되어 있음
- 미체결 지정가를 이후 시세 변화로 체결시키는 백그라운드 루프는 아직 없음

### 블록 5. 포인트와 상점

실현 손익 기반 포인트 적립과 `/shop` 구매 흐름을 만든다.
pay-to-win은 금지하고 cosmetic 보상으로만 시작한다.

수정 대상 후보:

- `backend/.../feature/reward/**`
- `frontend/app/(main)/shop/page.tsx`
- `frontend/components/.../ShopItemCard.tsx`
- `frontend/components/.../RewardPointCard.tsx`

현재 상태:

- `feature/reward` 최소 command/result/usecase/domain/infrastructure 골격은 생성됨
- 포인트 지급 구간에 대한 단위 테스트는 추가됨
- `GET /api/futures/rewards/me`, `GET /api/futures/shop/items`는 동작함
- 주문 종료 후 포인트 적립은 동작하지만 구매 처리 API는 아직 없음
- 프론트 `/shop`과 `/portfolio`는 reward/shop API를 실제로 읽도록 연결됨

## 구체적인 단계

1. 기준 문서를 다시 읽는다.
   루트에서:
   `sed -n '1,220p' FRONTEND.md`
   `sed -n '1,220p' BACKEND.md`

2. 프론트 신규 라우트와 타입 파일을 만든다.
   루트에서:
   `npm run build --workspace frontend`
   예상 결과:
   `Compiled successfully` 또는 타입 오류 목록

3. 백엔드 feature 골격을 만든다.
   `backend/`에서:
   `./gradlew test`
   예상 결과:
   기본 애플리케이션 테스트 통과

4. Bitget market connector와 account/order/position/reward feature를 순서대로 추가한다.

5. 프론트 주문 패널과 포트폴리오/상점 화면을 연결한다.

6. 아래 검증 명령을 반복 실행한다.
   루트에서:
   `npm run build --workspace frontend`
   `cd backend && ./gradlew architectureLint`
   `cd backend && ./gradlew check`

7. UI 영향이 생기면 브라우저로 `/markets`, `/markets/BTCUSDT`, `/portfolio`, `/shop`을 직접 검증한다.

## 검증과 수용 기준

### 실행 명령

- 루트:
  `npm run build --workspace frontend`
- 백엔드:
  `./gradlew architectureLint`
  `./gradlew check`

### 사용자 관점 수용 기준

1. 회원가입 후 로그인할 수 있다.
2. 로그인 후 `/markets`에서 `BTCUSDT`, `ETHUSDT`를 볼 수 있다.
3. `/markets/[symbol]`에서 최대 `50x`, `ISOLATED`/`CROSS`, maker/taker 예상 수수료를 읽을 수 있다.
4. 주문 제출 후 `/portfolio`에서 포지션과 손익이 반영된다.
5. 이익 실현 후 포인트가 적립되고 `/shop`에서 사용 가능하다.

### 테스트 관점 수용 기준

- 프론트 빌드가 통과한다
- 백엔드 `architectureLint`가 통과한다
- 백엔드 `check`가 통과한다
- 주문/포지션/수수료 계산을 검증하는 단위 테스트가 존재한다
- 포인트 지급 구간을 검증하는 단위 테스트가 존재한다

## 반복 실행 가능성 및 복구

### 반복 실행 시 안전성

- 프론트 빌드와 백엔드 테스트는 여러 번 실행해도 안전하다
- Bitget 조회는 읽기 전용 connector로 시작해야 하므로 외부 상태를 바꾸지 않는다

### 위험한 단계

- DB 스키마 추가
- 인증 구조 변경
- 주문/포지션 계산 로직 변경

### 롤백 또는 재시도 방법

- 프론트는 신규 파일 병행 생성 전략을 유지해 회귀를 줄인다
- 계산 엔진은 단위 테스트를 먼저 세운 뒤 수정한다
- DB 스키마를 바꿀 때는 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 함께 갱신한다

## 산출물과 메모

- 계획 문서:
  이 문서는 stale/superseded 종료 기록으로 `completed`에 보관된다.
- 제품 설계:
  [docs/product-specs/coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)
- 화면 명세:
  [docs/product-specs/coin-futures-screen-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-screen-spec.md)
- 계산 규칙:
  [docs/product-specs/coin-futures-simulation-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-simulation-rules.md)
- 프론트 빌드 성공:
  `npm run build --workspace frontend`
- 백엔드 검증 성공:
  `./gradlew architectureLint`
  `./gradlew test`

남은 TODO:

- 회원가입 인증 방식을 이메일 전용으로 갈지 OAuth를 붙일지 결정
- 교차 마진 청산 수식을 얼마나 현실적으로 가져갈지 결정
- shop 아이템 가격표 확정

## 인터페이스와 의존성

### 외부 의존성

- `Bitget` public market API
  이유:
  시세, 캔들, funding 정보를 가져오기 위한 단일 외부 소스다.

- `Redis`
  이유:
  짧은 TTL의 시세 스냅샷과 polling 캐시를 저장한다.

### 프론트에서 반드시 생겨야 하는 인터페이스

```ts
// frontend/type/market/market.ts
export type MarketSymbol = "BTCUSDT" | "ETHUSDT";

export type MarketTicker = {
  symbol: MarketSymbol;
  lastPrice: number;
  change24h: number;
  fundingRate: number;
  volume24h: number;
  updatedAt: string;
};
```

```ts
// frontend/type/trading/order.ts
export type CreateOrderRequest = {
  symbol: MarketSymbol;
  positionSide: "LONG" | "SHORT";
  orderType: "MARKET" | "LIMIT";
  marginMode: "ISOLATED" | "CROSS";
  leverage: number;
  quantity: number;
  limitPrice?: number;
};
```

### 백엔드 계약 예시와 현재 기준 변경 메모

아래 예시는 당시 초안에서 잡았던 계약 타입 예시다.
이제는 "반드시 인터페이스를 먼저 만든다"가 기준이 아니다.
최신 기준은 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)와 [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)를 따른다.
즉, 계층은 유지하되 `port`, `usecase` 인터페이스는 실제 경계가 있을 때만 도입한다.

```java
package coin.coinzzickmock.feature.market.application.port;

public interface LoadMarketSnapshotPort {
    MarketSnapshot load(String symbol);
}
```

```java
package coin.coinzzickmock.feature.order.application.usecase;

public interface CreateOrderUseCase {
    CreateOrderResult execute(CreateOrderCommand command);
}
```

```java
package coin.coinzzickmock.feature.reward.application.usecase;

public interface GrantProfitPointUseCase {
    void grant(GrantProfitPointCommand command);
}
```

## 변경 메모

- 2026-04-16:
  큰 목표를 구현 블록 단위로 쪼개기 위해 신규 작성했다.
  제품 설계, 화면 명세, 시뮬레이션 규칙 문서를 연결하고, 현재 저장소 상태와 바로 이어질 구현 순서를 한 문서 안에 고정했다.
- 2026-04-16:
  블록 1 구현 결과를 반영했다.
  새 `markets` 라우트와 공통 셸 전환, 시드 마켓 타입 추가, 미들웨어 업데이트, 프론트 빌드 통과 상태를 문서에 기록했다.
- 2026-04-16:
  백엔드 feature 골격 생성 결과를 반영했다.
  `common/api`, `providers`, `feature/account|market|order|position|reward` 초기 골격과 reward 포인트 테스트를 추가하고, `architectureLint`와 `test` 통과 상태를 기록했다.
- 2026-04-16:
  backend를 실제 futures API로 확장한 결과를 반영했다.
  Bitget ticker fallback connector, 인메모리 account/order/position/reward 저장소, 주문 preview/create, 포지션 조회/종료, 보상/상점 조회 API와 order 단위 테스트를 추가했다.
