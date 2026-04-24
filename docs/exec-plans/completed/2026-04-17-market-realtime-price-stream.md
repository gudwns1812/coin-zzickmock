# 마켓 최신가 실시간 스트림 전환

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서 하나만 읽어도 초보자가 현재 마켓 최신가 조회가 왜 단발성인지, 무엇을 어디에 추가해야 실시간에 가깝게 동작하는지, 그리고 어떤 순서로 검증해야 하는지 이해할 수 있게 유지한다.

## 목적 / 큰 그림

현재 코인 마켓 화면은 사용자가 `/markets` 또는 `/markets/[symbol]` 페이지를 열 때마다 백엔드가 Bitget REST ticker를 한 번 조회하고, 그 결과를 서버 렌더링 스냅샷으로만 보여 준다.
이 구조에서는 사용자가 페이지를 열어 둔 상태에서 가격이 바뀌어도 최신가와 주문 패널 기준가가 갱신되지 않는다.

이 작업이 끝나면 백엔드는 지원 심볼의 최신 ticker를 백그라운드에서 계속 갱신하고, 프론트는 서버가 내보내는 스트림을 구독해 상세 화면의 최신 체결가/Mark Price/Funding과 주문 패널 기준가를 자동으로 갱신한다.
사용자는 새로고침 없이도 현재 가격 변화를 확인할 수 있어야 하며, 주문 입력 기본가도 오래된 값에 머무르지 않아야 한다.
추가로 메인 `/markets` 랜딩에서도 BTCUSDT, ETHUSDT 카드가 최신가를 즉시 반영하고, 가격이 오르거나 내릴 때 배경색이 짧게 깜빡여 변화를 눈으로 알아차릴 수 있어야 한다.

## 진행 현황

- [x] (2026-04-17 00:00+09:00) 계획 초안 작성 완료
- [x] (2026-04-17 10:18+09:00) 사용자 승인 완료
- [x] (2026-04-17 10:24+09:00) `red` 단계 완료: 실시간 시세 캐시/스트림이 없음을 고정하는 테스트 추가 및 실패 확인
- [x] (2026-04-17 10:28+09:00) `green` 단계 완료: 백엔드 최신가 캐시와 SSE 엔드포인트 추가
- [x] (2026-04-17 10:29+09:00) `green` 단계 완료: 프론트 상세 화면과 주문 패널에 실시간 구독 연결
- [x] (2026-04-17 10:30+09:00) `refactor` 단계 완료: 상세 화면을 클라이언트 실시간 뷰로 분리하고 타입/매핑 중복 정리
- [x] (2026-04-17 10:31+09:00) 백엔드 검증 완료: `./gradlew architectureLint`, `./gradlew check`
- [x] (2026-04-17 10:32+09:00) 프론트 검증 완료: `npm run build --workspace frontend`
- [x] (2026-04-17 10:33+09:00) 런타임 검증 시도 완료: 초기 `bootRun`은 기존 Flyway migration 불일치로 실패해 환경 blocker를 확인
- [x] (2026-04-17 11:54+09:00) 런타임 검증 재완료: `bootRun`, `curl -N /api/futures/markets/BTCUSDT/stream`, 프론트 `/markets/BTCUSDT` 접속으로 SSE 연결과 최신가 갱신 확인
- [x] (2026-04-17 10:36+09:00) review gate 확인 완료: 변경 범위 검토에서 blocking issue 없음
- [x] (2026-04-17 11:44+09:00) 메인 `/markets` 랜딩을 SSE 최신가와 가격 변동 플래시 상태에 연결
- [x] (2026-04-17 11:49+09:00) 프론트 검증 재실행: `npm run build --workspace frontend` 통과
- [x] (2026-04-17 11:49+09:00) 런타임 검증 보강: 임시 `next dev --port 3100`에서 `/markets` 200 응답과 `/proxy-futures/markets/BTCUSDT/stream` SSE 이벤트 전달 확인
- [ ] PR 생성
- [x] (2026-04-17 15:24+09:00) 후속 조사 시작: `/markets` 랜딩에서 `BTCUSDT` SSE 콘솔 에러 제보를 받아 프록시 응답과 브라우저 연결 조건을 재검토
- [x] (2026-04-17 15:29+09:00) rewrite 기반 SSE 프록시를 전용 route handler로 대체해 랜딩/상세 공통 스트림 안정화
- [x] (2026-04-17 15:30+09:00) 프론트 빌드 및 로컬 SSE 런타임 검증 재실행
- [x] (2026-04-17 15:34+09:00) 변경 범위 review gate 재확인 완료: route handler + 프론트 SSE 경로 변경 기준 blocking issue 없음, 자동 프론트 테스트 부재는 빌드/런타임 검증으로 보완
- [ ] (2026-04-17 15:34+09:00) PR 생성

## 놀라움과 발견

- 관찰:
  현재 백엔드는 `BitgetMarketDataGateway`에서 HTTP ticker를 요청 시점마다 직접 호출한다.
  증거:
  [backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java) 의 `loadMarket`, `loadSupportedMarkets`.

- 관찰:
  프론트 마켓 화면은 서버 컴포넌트에서 `getFuturesMarket`, `getFuturesMarkets`를 호출해 초기 스냅샷만 렌더링한다.
  증거:
  [frontend/app/(main)/markets/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx),
  [frontend/app/(main)/markets/[symbol]/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/[symbol]/page.tsx),
  [frontend/app/(main)/watchlist/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/watchlist/page.tsx).

- 관찰:
  메인 `/markets` 랜딩의 `MarketsLanding`은 서버에서 받은 두 개의 `MarketSnapshot`을 그대로 렌더링하는 순수 서버 스냅샷이라, 상세 화면과 달리 SSE 구독이 없다.
  증거:
  [frontend/app/(main)/markets/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/page.tsx),
  [frontend/components/router/(main)/markets/MarketsLanding.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/router/(main)/markets/MarketsLanding.tsx).

- 관찰:
  기존에 떠 있던 `3000` 개발 서버는 `/markets`에 500을 반환했지만, 현재 작업 트리로 새로 띄운 `3100` 개발 서버는 동일 경로에 200을 반환했다. 즉 런타임 확인은 기존 장기 실행 서버가 아니라 이번 변경 기준의 새 프로세스로 재검증해야 했다.
  증거:
  `curl -I http://127.0.0.1:3000/markets` 는 `500 Internal Server Error`, `npm run dev --workspace frontend -- --port 3100` 후 `curl -I http://127.0.0.1:3100/markets` 는 `200 OK`.

- 관찰:
  주문 패널은 `defaultPrice`를 최초 state로만 복사하므로, 페이지를 열어 둔 뒤 가격이 바뀌어도 지정가 입력 초기값과 현재 기준가 안내가 갱신되지 않는다.
  증거:
  [frontend/components/futures/OrderEntryPanel.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/futures/OrderEntryPanel.tsx) 의 `useState(defaultPrice.toString())`.

- 관찰:
  프론트 `next.config.ts` rewrite를 거친 SSE 응답에는 백엔드 원본 응답에 없던 `connection: close` 헤더가 붙어 있었다. 백엔드 원본 스트림은 5초 동안 3개의 이벤트를 보냈지만, 동일 시간 창에서 rewrite 경유 응답은 2개만 내려와 브라우저 `EventSource`가 끊김으로 해석할 여지가 있었다.
  증거:
  `curl -i -N --max-time 5 http://127.0.0.1:8080/api/futures/markets/BTCUSDT/stream` 응답 헤더에는 `Content-Type: text/event-stream`, `Transfer-Encoding: chunked`만 있었고, `curl -i -N --max-time 5 http://127.0.0.1:3100/proxy-futures/markets/BTCUSDT/stream` 에는 `connection: close`가 추가됐다.

- 관찰:
  `next build`와 `next dev`가 같은 `frontend/.next`를 동시에 쓰면 `_buildManifest.js.tmp.*` 파일을 찾지 못하는 개발 서버 오류가 발생해 런타임 검증이 오염됐다.
  증거:
  개발 서버 로그에 `ENOENT: no such file or directory, open '.../frontend/.next/static/development/_buildManifest.js.tmp.*'` 가 반복 출력됐고, dev 서버를 내린 뒤 build를 다시 돌리자 정상 통과했다.

- 관찰:
  로컬 런타임 검증 첫 시도에서는 기존 MySQL의 Flyway 히스토리에 적용된 `V2` migration이 작업 트리와 맞지 않아 애플리케이션이 기동 전에 멈췄다. 이후 로컬 스키마 상태를 정리한 뒤 같은 검증을 재시도하자 정상 기동과 SSE 응답을 확인할 수 있었다.
  증거:
  첫 시도에서는 `./gradlew bootRun --console=plain` 실행 로그에 `Detected applied migration not resolved locally: 2.` 가 출력되며 종료됐다. 이후 재시도에서는 `curl -N http://127.0.0.1:8080/api/futures/markets/BTCUSDT/stream` 에서 반복 SSE 이벤트가 내려왔고, 프론트 `/markets/BTCUSDT`에서도 `/proxy-futures/markets/BTCUSDT/stream` 연결이 200으로 유지됐다.

## 의사결정 기록

- 결정:
  1차 구현은 Bitget WebSocket 직접 연결보다 "백엔드 폴링 캐시 + SSE(Server-Sent Events)" 조합으로 진행한다.
  근거:
  현재 백엔드는 `spring-boot-starter-web` 기반이며, 새 의존성 없이도 `@Scheduled`와 `SseEmitter`로 작은 범위의 실시간 갱신을 만들 수 있다.
  우선 최신가가 새로고침 없이 보이게 하는 것이 목적이므로, 거래소 소켓 프로토콜을 바로 도입하는 것보다 구현 위험과 검증 범위를 낮출 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  1차 실시간 적용 범위는 `/markets/[symbol]` 상세 화면과 주문 패널로 제한한다.
  근거:
  사용자 요구의 핵심은 "클라이언트 요청 시점에만 확인하는 구조"를 "최신가를 실시간으로 확인"하는 구조로 바꾸는 것이다.
  상세 화면이 주문 의사결정과 직접 연결되고, 주문 패널이 오래된 가격을 계속 들고 있는 리스크가 가장 크므로 먼저 여기부터 고정한다.
  목록/워치리스트는 같은 백엔드 스트림 모델을 재사용해 이후 확장할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  사용자 후속 요청에 따라 `/markets` 메인 랜딩도 같은 SSE 모델로 확장하되, 서버 초기 스냅샷 + 클라이언트 실시간 구독의 2단 구조는 유지한다.
  근거:
  메인 랜딩은 SEO나 초기 로딩 면에서 서버 렌더 스냅샷을 유지하는 편이 자연스럽고, 실시간 갱신은 브라우저에서만 필요하다.
  따라서 `page.tsx`는 그대로 서버 데이터 진입점으로 두고, 별도 클라이언트 래퍼가 두 심볼 스트림만 구독해 점진적으로 화면을 갱신하는 방식이 가장 작은 변경으로 요구를 만족한다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  브라우저 `EventSource`는 `next.config.ts` rewrite 대신 App Router의 전용 route handler를 통해 백엔드 SSE를 프록시한다.
  근거:
  이번 조사에서 rewrite 경유 응답에만 `connection: close` 헤더가 붙었고, 사용자 제보도 랜딩 SSE 끊김 콘솔 에러였다. 시장 스트림은 장시간 열린 연결이므로 일반 JSON 프록시와 같은 rewrite 경로에 기대기보다, `text/event-stream` 헤더와 `ReadableStream` 본문을 명시적으로 보존하는 route handler가 더 예측 가능하다. 이렇게 하면 기존 `/proxy-futures/*` rewrite는 주문/미리보기 같은 일반 HTTP 요청에만 남기고, 스트림 경로만 별도 안정화할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  SSE 초기 이벤트는 컨트롤러가 현재 스냅샷을 즉시 한 번 보내고, 애플리케이션 서비스는 이후 갱신 이벤트만 publish한다.
  근거:
  이렇게 나누면 `application` 서비스가 HTTP 전용 `SseEmitter`를 몰라도 되고, 스트림 어댑터 책임이 `api` 레이어에 남는다.
  동시에 구독 직후 첫 화면이 비어 보이지 않도록 현재 가격도 바로 전달할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  실시간 캐시/구독 책임은 `application/service`가 아니라 `application.realtime.MarketRealtimeFeed`로 분리한다.
  근거:
  `GetMarketSummaryService`가 다른 service를 직접 참조하면 유스케이스 경계가 흐려진다.
  실시간 캐시와 구독은 여러 유스케이스가 함께 쓰는 메커니즘이므로 비-Service application 협력 객체가 더 맞다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- 백엔드는 `MarketRealtimeFeed`를 추가해 지원 심볼의 최신 ticker를 메모리 캐시에 유지하고, `GET /api/futures/markets/{symbol}/stream` SSE 엔드포인트로 후속 갱신을 흘려보내도록 바뀌었다.
- 프론트는 상세 페이지를 클라이언트 실시간 뷰로 분리해 새로고침 없이 가격 표시와 주문 패널 기준가를 갱신한다.
- 프론트는 상세 페이지에 더해 메인 `/markets` 랜딩도 클라이언트 실시간 뷰로 감싸 새로고침 없이 BTC/ETH 카드 가격과 메트릭을 갱신하고, 가격 방향에 따라 짧은 배경 플래시를 준다.
- `./gradlew architectureLint`, `./gradlew check`, `npm run build --workspace frontend`는 모두 통과했다.
- 변경 범위 review gate에서는 가독성/성능/보안/테스트/아키텍처 관점의 blocking issue를 찾지 못했다.
- 이후 로컬 DB migration 이력을 정리한 뒤 `bootRun`을 다시 올려 `curl -N http://127.0.0.1:8080/api/futures/markets/BTCUSDT/stream`에서 반복 SSE 이벤트를 확인했고, 프론트 `/markets/BTCUSDT`에서도 `/proxy-futures/markets/BTCUSDT/stream` 연결이 200으로 유지되는 것을 확인했다.
- 메인 랜딩 후속 검증에서는 임시 `next dev --port 3100`로 현재 변경을 띄운 뒤 `GET /markets 200`, `curl -N http://127.0.0.1:3100/proxy-futures/markets/BTCUSDT/stream`의 연속 이벤트를 확인했다.
- 후속 버그픽스에서는 프론트 SSE 구독을 `frontend/app/api/futures/markets/[symbol]/stream/route.ts` 전용 route handler로 옮겼고, `curl -i -N --max-time 5 http://127.0.0.1:3100/api/futures/markets/BTCUSDT/stream` 응답에서 `200 text/event-stream`, `cache-control: no-cache, no-transform`, `x-accel-buffering: no`와 5초 창의 반복 이벤트를 확인했다.
- 같은 dev 서버에서 `GET /markets 200`, `GET /api/futures/markets/BTCUSDT/stream 200`, `GET /api/futures/markets/ETHUSDT/stream 200`을 확인했고, 두 심볼 스트림 모두 5초 창에서 3회 이벤트를 유지했다.
- 후속 버그픽스 review gate 재확인에서는 route handler와 두 `EventSource` 경로 변경만 범위로 잡아 관련 관점을 점검했다. blocking issue는 없었고, 프론트 자동 테스트 인프라가 없는 대신 `npm run build --workspace frontend` 와 실제 SSE `curl` 검증으로 회귀 위험을 보완했다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

관련 코드 경로:

- [backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java)
- [frontend/lib/futures-api.ts](/Users/hj.park/projects/coin-zzickmock/frontend/lib/futures-api.ts)
- [frontend/app/(main)/markets/[symbol]/page.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/app/(main)/markets/[symbol]/page.tsx)
- [frontend/components/futures/OrderEntryPanel.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/futures/OrderEntryPanel.tsx)

선행 조건:

- 지원 심볼은 현재 `BTCUSDT`, `ETHUSDT` 두 개다.
- 백엔드는 `Providers -> ConnectorProvider -> MarketDataGateway` 경계를 유지해야 한다.
- 프론트는 초기 데이터는 서버 컴포넌트에서 받고, 실시간 갱신만 클라이언트 컴포넌트로 내려야 한다.
- 기존 워킹트리에 사용자 문서/계획 변경이 있으므로, 이번 작업은 해당 변경을 되돌리지 않고 공존해야 한다.

## 작업 계획

먼저 `red` 단계에서 백엔드 서비스 계층에 "지원 심볼 최신가 캐시가 주기적으로 갱신되고 현재 스냅샷을 반환한다"는 동작 테스트와, 스트림 엔드포인트가 최신가를 내보낸다는 최소 단위 테스트를 추가한다.
이 테스트는 현재 구조에서는 실패해야 하며, 단발성 REST 호출만 있는 상태를 고정한다.

그 다음 `green` 단계에서 백엔드에 실시간 시세 런타임 블록을 추가한다.
구체적으로는 `feature/market/application` 아래에 지원 심볼 최신가 캐시를 관리하는 서비스와 결과 모델을 두고, `providers.connector().marketDataGateway()`를 이용해 주기적으로 ticker를 가져와 메모리에 반영한다.
동시에 `feature/market/api`에 SSE 엔드포인트를 추가해 특정 심볼의 최신 스냅샷을 스트림으로 흘려보낸다.
컨트롤러는 스트림 DTO 매핑만 담당하고, 캐시 갱신/구독 관리의 원본 책임은 `application` 서비스가 가진다.

프론트는 서버 컴포넌트가 초기 스냅샷을 그대로 렌더링하되, 상세 페이지 내부에 클라이언트 컴포넌트를 한 겹 두어 `EventSource`로 `/api/futures/markets/{symbol}/stream`을 구독하게 만든다.
이 컴포넌트는 최신 체결가/Mark/Funding 표시와 `OrderEntryPanel`의 기준가를 함께 갱신해야 한다.
`OrderEntryPanel`은 외부에서 최신가를 prop으로 받을 때, 사용자가 직접 수정 중이 아닌 경우에만 지정가 입력 초기값을 동기화해 갑작스러운 사용자 입력 덮어쓰기를 막는다.

이번 후속 수정에서는 같은 원칙을 메인 `/markets` 랜딩에도 적용한다.
`frontend/app/(main)/markets/page.tsx`는 그대로 초기 `MarketSnapshot` 두 개를 준비하고, 새 클라이언트 래퍼가 BTCUSDT와 ETHUSDT의 SSE를 각각 구독해 `MarketsLanding`에 최신 스냅샷을 전달한다.
가격 비교는 직전 `lastPrice`와 새 `lastPrice`를 기준으로 계산하고, 값이 오르면 청록/파랑 계열, 내리면 빨강 계열 배경을 약 1초 미만 짧게 보여 주어 메인 화면에서도 변화를 즉시 알아볼 수 있게 한다.

마지막 `refactor` 단계에서 백엔드 DTO 매핑과 프론트 실시간 스냅샷 상태 로직의 중복을 정리하고, 테스트가 모두 초록 상태인 것을 다시 확인한다.

이번 후속 버그픽스에서는 프론트 `frontend/app/api/futures/markets/[symbol]/stream/route.ts` 에 전용 SSE 프록시 route handler를 추가한다.
이 handler는 브라우저 요청 쿠키를 필요 시 백엔드로 전달하고, 백엔드 `ReadableStream` 본문과 `text/event-stream`, `cache-control`, `x-accel-buffering` 같은 스트림 헤더를 그대로 유지한 채 응답한다.
기존 컴포넌트의 `EventSource` 호출 경로는 `/api/futures/markets/{symbol}/stream` 으로 바꾸고, rewrite가 아니라 App Router route handler가 직접 스트림을 전달하도록 만들어 랜딩/상세가 동시에 안정화되게 만든다.

## 구체적인 단계

1. 백엔드 현재 시장 서비스와 테스트 위치를 읽고, 실시간 캐시/스트림용 테스트 파일을 추가한다.
2. 테스트를 실행해 실패를 확인한다.
3. 백엔드 `feature.market.application`과 `feature.market.api`에 최신가 캐시, 스케줄 갱신, SSE 스트림을 구현한다.
4. 프론트에 실시간 마켓 뷰 컴포넌트와 SSE 구독 훅 또는 클라이언트 로직을 추가한다.
5. 상세 페이지가 새 클라이언트 뷰를 사용하도록 연결한다.
6. 메인 `/markets` 페이지가 새 클라이언트 실시간 랜딩 뷰를 사용하도록 연결하고 가격 변동 플래시 상태를 추가한다.
7. `./gradlew architectureLint`, `./gradlew check`, `npm run build --workspace frontend`를 실행한다.
8. 변경 범위만 대상으로 `AGENTS.md` 기준 review 스킬 검토를 수행한다.
9. 랜딩 SSE 후속 버그픽스로 `frontend/app/api/futures/markets/[symbol]/stream/route.ts` 를 추가하고, `curl -i -N` 으로 프론트 route handler 경유 응답 헤더와 이벤트 전달을 다시 확인한다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.market...`
- `cd backend && ./gradlew architectureLint`
- `cd backend && ./gradlew check`
- `npm run build --workspace frontend`

수용 기준:

- `/api/futures/markets/{symbol}`는 기존처럼 단발 조회 응답을 계속 제공한다.
- `/api/futures/markets/{symbol}/stream`에 연결하면 일정 주기마다 최신 스냅샷 이벤트가 전달된다.
- `/markets/BTCUSDT` 또는 `/markets/ETHUSDT` 화면을 열어 둔 채 가격이 바뀌면 "최신 체결가", "Mark Price", "Funding" 값이 새로고침 없이 갱신된다.
- `/markets` 메인 랜딩을 열어 둔 채 가격이 바뀌면 BTCUSDT/ETHUSDT 카드의 최신 체결가, Mark Price, Funding, 24h 변화율이 새로고침 없이 갱신된다.
- 메인 랜딩에서 가격이 오르면 상승 계열, 내리면 하락 계열 배경이 짧게 깜빡여 변화를 시각적으로 알린다.
- 주문 패널의 "현재 기준가"가 최신가를 따라가고, 사용자가 지정가를 직접 수정하지 않은 상태라면 기본 입력값도 함께 따라간다.
- 사용자가 지정가를 직접 수정한 뒤에는 들어오던 스트림이 그 입력값을 강제로 덮어쓰지 않는다.

## 반복 실행 가능성 및 복구

- 메모리 캐시와 SSE 연결은 애플리케이션 재시작 시 초기화되어도 된다. 영속 스토리지는 추가하지 않는다.
- 스트림 연결이 실패해도 프론트는 서버 렌더 초기 스냅샷을 계속 보여 줘야 한다. 실시간 연결 실패가 페이지 전체 실패로 번지면 안 된다.
- 폴링 주기는 상수 또는 설정값 한 곳에서 관리해, 너무 촘촘하면 쉽게 완화할 수 있어야 한다.

## 산출물과 메모

- 예상 PR 제목:
  최신가 실시간 스트림 연결
- 남은 TODO:
  목록(`/markets`, `/watchlist`)까지 같은 스트림 모델로 확장할지 후속 작업에서 판단
