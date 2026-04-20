# 최신 시세 기반 1분봉·1시간봉 저장

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다. 이 문서는 살아 있는 문서이며, `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고`를 작업 내내 최신 상태로 유지한다.

## 목적 / 큰 그림

현재 백엔드는 `BTCUSDT`, `ETHUSDT`의 최신 ticker를 주기적으로 가져와 화면 최신가에는 반영하지만, 이미 만들어 둔 `market_candles_1m`, `market_candles_1h` 테이블에는 아무 데이터도 저장하지 않는다. 이 상태에서는 과거 가격 차트 API를 붙이려 해도 사용할 히스토리 원본이 비어 있다.

이 작업이 끝나면 최신 ticker 수집 시점마다 `lastPrice`를 분 단위 OHLC로 가공해 `market_candles_1m`에 upsert하고, 같은 시점의 시간 구간 `1m` 데이터를 다시 집계해 `market_candles_1h`에도 upsert한다. 사용자는 이후 차트 조회 기능을 붙일 때 최소한의 `1m`, `1h` 가격 히스토리를 이미 DB에서 읽을 수 있어야 한다.

## 진행 현황

- [x] (2026-04-17 13:35+09:00) 사용자 요구와 현재 시세 수집/시장 히스토리 스키마 구조 확인 완료
- [x] (2026-04-17 13:41+09:00) 구현 방향 승인 완료
- [x] (2026-04-17 15:32+09:00) `red` 단계 완료: refresh 후 `market_candles_1m`, `market_candles_1h`가 채워져야 한다는 H2 통합 테스트 추가 및 실패 확인
- [x] (2026-04-17 15:36+09:00) `green` 단계 완료: 시장 히스토리 영속 계층과 최신가 가공 저장 구현
- [x] (2026-04-17 15:36+09:00) `refactor` 단계 완료: 시간 버킷/롤업 로직 분리와 in-memory 테스트 보강
- [x] (2026-04-17 15:37+09:00) 검증 완료: `./gradlew architectureLint`, `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check`
- [ ] review gate 확인 및 PR 단계 진행

## 놀라움과 발견

- 관찰:
  현재 저장소에는 `market_candles_1m`, `market_candles_1h` 스키마와 문서만 있고, 이를 읽고 쓰는 `feature.market` 영속 코드가 없다.
  증거:
  [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql), [db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)

- 관찰:
  최신 시세 수집 소스는 현재 Bitget ticker 하나뿐이라 거래량 계열 값을 정확히 계산할 원본이 없다.
  증거:
  [BitgetMarketDataGateway.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java)

- 관찰:
  `check` 전체 실행에서는 `@Scheduled`가 테스트 중간에도 refresh를 태워 저장소 개수 검증이 흔들렸다.
  증거:
  `JpaMarketHistoryRepositoryTest`가 단독 실행에서는 통과했지만 전체 `./gradlew check`에서는 row count가 증가해 실패했고, 테스트 속성에 `spring.task.scheduling.enabled=false`를 넣은 뒤 안정화됐다.

## 의사결정 기록

- 결정:
  이번 1차 구현에서는 최신 ticker의 `lastPrice`만 사용해 `1m` 분봉을 누적하고, `1h`는 저장된 `1m`를 다시 집계해 만든다.
  근거:
  현재 커넥터가 제공하는 외부 데이터가 ticker뿐이라 추가 외부 계약 확장 없이 바로 붙일 수 있는 안전한 경로다. 사용자 요구도 “최신 데이터를 수집할 때 가공해서 DB에 저장”이므로 우선 가격 히스토리 축을 비우지 않는 것이 핵심이다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  거래량, quote volume, trade count는 현재 소스 한계 때문에 우선 `0`으로 저장한다.
  근거:
  `null`을 섞으면 이후 집계 로직이 불필요하게 복잡해지고, 현재 제품 요구는 우선 가격 히스토리 축을 만드는 것이다. 추후 candle 전용 커넥터를 붙이면 같은 필드를 실제 값으로 대체할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  시간 버킷은 `UTC` 기준 `1m`, `1h`로 정규화한다.
  근거:
  `Instant` 중심 저장과 가장 자연스럽고, 거래소 시계열과도 지역 시간보다 잘 맞는다. 향후 `1D`, `1W`, `1M` 경계 정책을 고정할 때도 기준축을 분명히 유지할 수 있다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- `MarketRealtimeFeed`가 최신 ticker refresh 이후 `MarketHistoryRecorder`를 호출해 `1m`, `1h` 캔들을 함께 upsert하도록 바뀌었다.
- `feature.market.infrastructure.persistence`에 심볼/분봉/시간봉 JPA 영속 계층을 추가해 기존 스키마를 실제로 사용하기 시작했다.
- `MarketRealtimeFeedTest`, `MarketRealtimeFeedPersistenceTest`, `JpaMarketHistoryRepositoryTest`를 통해 메모리 집계와 H2 영속 경로를 함께 검증했다.
- 이 문서 작성 시점에는 `tradeCount`를 `0`으로 저장했지만, 이후 스키마와 제품 스펙에서 `trade_count` 자체를 제거했다. 최신 기준은 `docs/product-specs/coin-futures-candle-timeframe-spec.md`와 `docs/generated/db-schema.md`를 따른다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
- [docs/product-specs/coin-futures-candle-timeframe-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-candle-timeframe-spec.md)

관련 코드:

- [MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)
- [GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [BitgetMarketDataGateway.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/providers/infrastructure/BitgetMarketDataGateway.java)
- [V3__add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V3__add_market_history_schema.sql)

현재 구조 설명:

- `MarketRealtimeFeed`는 3초마다 지원 심볼 ticker를 읽고 메모리 캐시/SSE 구독자에게만 반영한다.
- `market_symbols`에는 `BTCUSDT`, `ETHUSDT`가 이미 시드되어 있다.
- `market_candles_1m`, `market_candles_1h`는 `(symbol_id, open_time)` 유니크 키를 가지므로 같은 버킷은 upsert 형태로 저장하는 것이 자연스럽다.

## 작업 계획

먼저 `red` 단계에서 최신 시세 refresh가 분봉/시간봉을 저장해야 한다는 테스트를 추가한다. 이 테스트는 같은 분 안에서 가격이 바뀌면 `open/high/low/close`가 갱신되고, 다음 분 또는 다음 시세가 들어오면 시간봉 집계도 함께 바뀌는지를 고정해야 한다.

그 다음 `green` 단계에서 `feature.market`에 최소 영속 계층을 추가한다. `application`에는 시장 히스토리 저장 계약과 최신 시세를 `1m`, `1h`로 가공하는 recorder를 두고, `infrastructure/persistence`에는 심볼/분봉/시간봉 JPA 엔티티와 Spring Data 저장소, 그리고 계약 구현체를 둔다. `MarketRealtimeFeed`는 refresh된 최신 시세 목록을 캐시에 반영한 뒤 recorder에 넘겨 DB 저장도 함께 수행한다.

마지막 `refactor` 단계에서는 시간 버킷 계산, 분봉 갱신, 시간봉 롤업 코드를 작은 메서드로 정리하고, 테스트 픽스처 중복을 줄인다. 문서에는 이번 구현 한계인 “거래량은 아직 실제 거래소 candle 소스가 없어 0으로 저장됨”을 결과 및 회고에 남긴다.

## 구체적인 단계

1. `feature.market.application.realtime` 테스트에 히스토리 저장 시나리오를 추가한다.
2. 실패하는 테스트를 실행해 현재 기능이 없음을 확인한다.
3. `feature.market.application.repository`와 `feature.market.domain`에 시장 히스토리 계약/모델을 추가한다.
4. `feature.market.infrastructure.persistence`에 JPA 엔티티와 저장소 구현을 추가한다.
5. `MarketRealtimeFeed`가 refresh 시점에 히스토리 recorder를 호출하도록 수정한다.
6. H2 기반 저장소/집계 테스트와 기존 시장 테스트를 다시 돌린다.
7. `./gradlew architectureLint`, `./gradlew check`를 실행한다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedTest --console=plain`
- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.market.infrastructure.persistence.JpaMarketHistoryRepositoryTest --console=plain`
- `cd backend && ./gradlew architectureLint --console=plain`
- `cd backend && ./gradlew check --console=plain`

수용 기준:

- 최신 ticker refresh를 두 번 이상 수행하면 `market_candles_1m`에 해당 분 버킷의 OHLC가 저장된다.
- 같은 분 안에서 가격이 여러 번 바뀌면 `open`은 첫 값, `high/low`는 구간 extrema, `close`는 마지막 값으로 유지된다.
- 같은 시간 구간의 `1m` 누적 결과를 기준으로 `market_candles_1h`가 upsert된다.
- 저장 대상은 현재 지원 심볼 `BTCUSDT`, `ETHUSDT`에 한정된다.
- 거래량 계열은 현재 구현에서 `0`으로 저장되며, 이후 실제 candle 소스 도입 전까지 일관되게 집계된다.

## 반복 실행 가능성 및 복구

- 같은 시세 버킷에 대해 여러 번 refresh를 호출해도 `(symbol_id, open_time)` 유니크 키를 기준으로 동일 캔들이 갱신되어야 한다.
- 히스토리 저장이 실패해도 기존 최신가 캐시/SSE 기능은 코드상 분리되어 있어야 하며, 실패 원인은 로그로 추적 가능해야 한다.
- 스키마 변경은 이번 범위에 포함하지 않으므로 새 Flyway migration은 만들지 않는다.

## 산출물과 메모

- 예상 핵심 변경 파일:
  `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java`
  `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java`
  `backend/src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/*`
  `backend/src/test/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeedTest.java`

## 인터페이스와 의존성

새로 추가할 계약 후보:

- `coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository`
- `coin.coinzzickmock.feature.market.domain.MarketHistoryCandle`
- `coin.coinzzickmock.feature.market.domain.HourlyMarketCandle`
- `coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRecorder`

`MarketHistoryRepository`는 심볼 ID 조회, `1m` upsert, 특정 시간 구간 `1m` 조회, `1h` upsert를 제공한다. 구현은 `feature.market.infrastructure.persistence.JpaMarketHistoryRepository`가 맡고, 외부 의존성은 기존 `Spring Data JPA`만 사용한다.

## 변경 메모

- 2026-04-17 13:45+09:00 / Codex
  최신 ticker를 `1m`, `1h` 히스토리로 가공 저장하는 작업 계획을 신규 작성했다.
- 2026-04-17 15:38+09:00 / Codex
  구현/검증 결과와 스케줄링 테스트 안정화 내용을 반영해 계획서를 최신 상태로 갱신했다.
