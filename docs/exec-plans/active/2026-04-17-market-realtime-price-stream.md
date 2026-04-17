# 마켓 최신가 실시간 스트림 전환

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서 하나만 읽어도 초보자가 현재 마켓 최신가 조회가 왜 단발성인지, 무엇을 어디에 추가해야 실시간에 가깝게 동작하는지, 그리고 어떤 순서로 검증해야 하는지 이해할 수 있게 유지한다.

## 목적 / 큰 그림

현재 코인 마켓 화면은 사용자가 `/markets` 또는 `/markets/[symbol]` 페이지를 열 때마다 백엔드가 Bitget REST ticker를 한 번 조회하고, 그 결과를 서버 렌더링 스냅샷으로만 보여 준다.
이 구조에서는 사용자가 페이지를 열어 둔 상태에서 가격이 바뀌어도 최신가와 주문 패널 기준가가 갱신되지 않는다.

이 작업이 끝나면 백엔드는 지원 심볼의 최신 ticker를 백그라운드에서 계속 갱신하고, 프론트는 서버가 내보내는 스트림을 구독해 상세 화면의 최신 체결가/Mark Price/Funding과 주문 패널 기준가를 자동으로 갱신한다.
사용자는 새로고침 없이도 현재 가격 변화를 확인할 수 있어야 하며, 주문 입력 기본가도 오래된 값에 머무르지 않아야 한다.

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
- [x] (2026-04-17 10:36+09:00) 품질 점수 확인 완료: 변경 범위 수동 교차 검토에서 blocking issue 없음, final score 89로 기록
- [ ] PR 생성

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
  주문 패널은 `defaultPrice`를 최초 state로만 복사하므로, 페이지를 열어 둔 뒤 가격이 바뀌어도 지정가 입력 초기값과 현재 기준가 안내가 갱신되지 않는다.
  증거:
  [frontend/components/futures/OrderEntryPanel.tsx](/Users/hj.park/projects/coin-zzickmock/frontend/components/futures/OrderEntryPanel.tsx) 의 `useState(defaultPrice.toString())`.

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
  SSE 초기 이벤트는 컨트롤러가 현재 스냅샷을 즉시 한 번 보내고, 애플리케이션 서비스는 이후 갱신 이벤트만 publish한다.
  근거:
  이렇게 나누면 `application` 서비스가 HTTP 전용 `SseEmitter`를 몰라도 되고, 스트림 어댑터 책임이 `api` 레이어에 남는다.
  동시에 구독 직후 첫 화면이 비어 보이지 않도록 현재 가격도 바로 전달할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- 백엔드는 `MarketRealtimeService`를 추가해 지원 심볼의 최신 ticker를 메모리 캐시에 유지하고, `GET /api/futures/markets/{symbol}/stream` SSE 엔드포인트로 후속 갱신을 흘려보내도록 바뀌었다.
- 프론트는 상세 페이지를 클라이언트 실시간 뷰로 분리해 새로고침 없이 가격 표시와 주문 패널 기준가를 갱신한다.
- `./gradlew architectureLint`, `./gradlew check`, `npm run build --workspace frontend`는 모두 통과했다.
- 변경 범위 기준 수동 품질 검토에서는 readability/performance/security/test/architecture 관점의 blocking issue를 찾지 못했고, 기록용 final score는 89로 남긴다.
- 이후 로컬 DB migration 이력을 정리한 뒤 `bootRun`을 다시 올려 `curl -N http://127.0.0.1:8080/api/futures/markets/BTCUSDT/stream`에서 반복 SSE 이벤트를 확인했고, 프론트 `/markets/BTCUSDT`에서도 `/proxy-futures/markets/BTCUSDT/stream` 연결이 200으로 유지되는 것을 확인했다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [FRONTEND.md](/Users/hj.park/projects/coin-zzickmock/FRONTEND.md)
- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

관련 코드 경로:

- [backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
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

마지막 `refactor` 단계에서 백엔드 DTO 매핑과 프론트 실시간 스냅샷 상태 로직의 중복을 정리하고, 테스트가 모두 초록 상태인 것을 다시 확인한다.

## 구체적인 단계

1. 백엔드 현재 시장 서비스와 테스트 위치를 읽고, 실시간 캐시/스트림용 테스트 파일을 추가한다.
2. 테스트를 실행해 실패를 확인한다.
3. 백엔드 `feature.market.application`과 `feature.market.api`에 최신가 캐시, 스케줄 갱신, SSE 스트림을 구현한다.
4. 프론트에 실시간 마켓 뷰 컴포넌트와 SSE 구독 훅 또는 클라이언트 로직을 추가한다.
5. 상세 페이지가 새 클라이언트 뷰를 사용하도록 연결한다.
6. `./gradlew architectureLint`, `./gradlew check`, `npm run build --workspace frontend`를 실행한다.
7. 변경 범위만 대상으로 `multi-angle-review`를 수행하고 점수를 계산한다.

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
