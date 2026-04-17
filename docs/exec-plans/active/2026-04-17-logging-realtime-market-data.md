# 실시간 마켓 데이터 로깅 추가

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.

## 목적 / 큰 그림

현재 백엔드는 `MarketRealtimeFeed`를 통해 실시간 마켓 데이터를 캐시하고 SSE로 클라이언트에 스트리밍하고 있다.
하지만 서버 내부에서 실제로 어떤 데이터가 어떤 타이밍에 발행되는지 로그로 남지 않아, SSE 연동 문제를 디버깅하거나 데이터 흐름을 추적하기 어렵다.
사용자 요청에 따라 `MarketRealtimeFeed`가 새로운 데이터를 캐싱하고 구독자에게 발행할 때 `log.debug`를 남겨 런타임 가시성을 확보한다.

## 진행 현황

- [ ] (2026-04-17) 계획 초안 작성
- [ ] 사용자 승인
- [ ] `red` 단계: 로깅 여부 또는 발행 로직을 검증하는 테스트 확인
- [ ] `green` 단계: `MarketRealtimeFeed`에 SLF4J 로깅 추가 (`log.debug`)
- [ ] `refactor` 단계: 로깅 메시지 형식 및 성능 영향 검토
- [ ] 품질 점수 확인 및 PR 생성

## 의사결정 기록

- 결정:
  `log.debug`를 사용하여 실시간 데이터 발행 시점을 기록한다.
  근거:
  실시간 데이터는 빈도가 높으므로 `info` 레벨보다는 `debug` 레벨이 적절하며, 운영 환경에서는 필요할 때만 켜서 확인할 수 있게 한다.
  날짜/작성자:
  2026-04-17 / Codex

## 작업 계획

1. `MarketRealtimeFeed.java`에 `@Slf4j` 어노테이션을 추가하거나 `LoggerFactory`를 통해 Logger를 정의한다.
2. `cacheAndPublish` 메서드 내부에서 데이터를 캐시에 넣고 구독자에게 발행하기 직전 또는 직후에 `log.debug`를 추가한다.
3. 로그 메시지에는 심볼(symbol)과 주요 가격(lastPrice) 정보를 포함하여 어떤 데이터가 나가는지 식별할 수 있게 한다.
4. 기존 테스트 코드에서 발행 로직이 여전히 정상 동작하는지 확인한다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedTest`

수용 기준:

- `MarketRealtimeFeed`가 데이터를 발행할 때 `DEBUG` 레벨 로그가 출력된다.
- 로그 예시: `[DEBUG] Publish market data: MarketSummaryResult[symbol=BTCUSDT, lastPrice=...]`
- 기존 SSE 스트리밍 기능에 영향이 없어야 한다.
