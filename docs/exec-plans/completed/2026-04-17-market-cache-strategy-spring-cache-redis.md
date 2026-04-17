# 마켓 캐시 전략을 Spring Cache / Redis로 정리

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 현재 백엔드의 `ConcurrentHashMap` 기반 로컬 캐시를 Spring Cache 추상화로 바꾸고, 앞으로 분산 캐시가 필요할 때 Redis를 표준 경로로 쓰도록 설정과 코드를 함께 정리하는 단일 기준서다.
사용자 요청이 곧 작업 승인 신호이므로, 본 문서는 승인 직후 상태로 `active`에 둔다.

## 목적 / 큰 그림

지금 `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java` 는 최신 시세와 지원 심볼 목록을 클래스 내부 `ConcurrentHashMap` 과 `volatile` 필드에 직접 들고 있다.
이 방식은 당장 동작은 하지만, 캐시 정책이 코드 안에 박혀 있어 Spring의 캐시 추상화와 운영 설정을 활용하지 못하고, 나중에 분산 캐시가 필요한 기능과도 기준이 분리된다.

이 작업이 끝나면 로컬 최신 시세 캐시는 Spring Cache의 local `CacheManager` 뒤로 숨겨지고, `MarketRealtimeFeed` 는 캐시 저장소 구현 대신 캐시 역할에만 집중한다.
동시에 Redis 기반 distributed `CacheManager` 를 설정으로 켤 수 있게 만들어, 분산 캐시가 필요한 기능은 같은 Spring Cache 추상화 위에서 Redis를 표준 구현으로 사용할 수 있어야 한다.
검증은 "마켓 실시간 피드가 Spring local cache에 최신 시세를 기록하는지"와 "Redis 사용 설정을 켰을 때 distributed cache manager 빈이 생기는지"로 확인한다.

## 진행 현황

- [x] (2026-04-17 18:05+09:00) 작업 범위 확인 완료: `MarketRealtimeFeed` 내부 `ConcurrentHashMap` 이 현재 로컬 캐시 실사용처임을 확인
- [x] (2026-04-17 18:08+09:00) 계획 문서 작성 및 사용자 직접 요청을 승인 신호로 기록
- [x] (2026-04-17 20:43+09:00) `red` 단계 완료: local Spring cache 기록 테스트와 Redis distributed cache manager 생성 테스트를 추가하고 실패 확인
- [x] (2026-04-17 21:05+09:00) `green` 단계 완료: Spring Cache local wrapper, Redis cache configuration, market realtime feed 전환
- [x] (2026-04-17 21:08+09:00) `refactor` 단계 완료: cache name/property 상수, 테스트 helper, 설정 기본값 정리
- [x] (2026-04-17 21:12+09:00) 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check` 통과
- [x] (2026-04-17 21:14+09:00) 품질 점수 확인 완료: 변경 범위 수동 5각도 점검에서 blocker 없음, final score 93 기록
- [x] (2026-04-17 21:22+09:00) PR 생성 완료: [#14 로컬 캐시는 Spring Cache로, 분산 캐시는 Redis로 정리한다](https://github.com/gudwns1812/coin-zzickmock/pull/14)

## 놀라움과 발견

- 관찰:
  현재 저장소에서 `ConcurrentHashMap` 기반 캐시 실사용은 `MarketRealtimeFeed` 한 곳으로 모여 있다.
  증거:
  `rg -n "ConcurrentHashMap|CacheManager|@Cacheable" backend/src/main/java backend/src/test/java -S` 결과에서 `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java` 만 실제 캐시 상태를 직접 가진다.

- 관찰:
  Redis 관련 의존성이나 `CacheManager` 구성은 아직 없다.
  증거:
  [backend/build.gradle](/Users/hj.park/projects/coin-zzickmock/backend/build.gradle) 에 `spring-boot-starter-cache`, `spring-boot-starter-data-redis` 가 없고, `backend/src/main/resources/application.yml` 에도 cache/redis 설정이 없다.

- 관찰:
  YAML 값 끝의 `::` 는 따옴표가 없으면 설정 파싱이 깨질 수 있다.
  증거:
  첫 구현 후 `MarketRealtimeFeedCacheTest` 컨텍스트 기동이 `org.yaml.snakeyaml.scanner.ScannerException` 으로 실패했고, `key-prefix` 값을 `"coinzzickmock::"` 형태로 감싼 뒤 정상 기동했다.

## 의사결정 기록

- 결정:
  local cache는 `@Cacheable` annotation보다 programmatic Spring Cache wrapper로 감싼다.
  근거:
  `MarketRealtimeFeed` 는 주기 갱신 시 여러 심볼을 한 번에 갱신하고, SSE publish 직전에도 같은 저장소를 읽는다. 이런 구조는 단일 메서드 호출 기준 annotation보다 `CacheManager` 와 `Cache` 를 직접 쓰는 편이 갱신/조회 흐름을 더 명확하게 표현한다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  분산 캐시는 "지금 바로 market realtime feed에 억지로 적용"하지 않고, Redis `CacheManager` 를 설정으로 켤 수 있게 만드는 수준으로 정리한다.
  근거:
  현재 `MarketRealtimeFeed` 의 subscriber registry 는 SSE 연결 객체를 들고 있어 단일 노드 메커니즘이 강하다. 분산 캐시는 향후 다른 읽기 중심 기능에서 쓸 가능성이 크므로, 먼저 표준 Redis 경로를 구성하고 현재 로컬 캐시 사용처만 Spring Cache로 옮기는 편이 범위를 닫기 쉽다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- `MarketRealtimeFeed` 는 더 이상 최신 시세와 지원 심볼을 클래스 내부 `ConcurrentHashMap`/`volatile` 필드에 직접 들고 있지 않고, `MarketRealtimeLocalCache` 를 통해 Spring Cache local `CacheManager` 에 기록한다.
- backend 전역에는 `localCacheManager` 와 조건부 `distributedCacheManager` 가 생겼다. local cache 는 기본 활성이고, Redis distributed cache 는 `coin.cache.redis.enabled=true` 일 때만 빈이 만들어진다.
- `spring-boot-starter-cache`, `spring-boot-starter-data-redis`, `@EnableCaching`, 기본 `coin.cache.redis.*` 설정을 추가해 이후 분산 캐시가 필요한 기능이 같은 Spring Cache 추상화 위에서 Redis를 표준 구현으로 선택할 수 있게 했다.
- `MarketRealtimeFeedCacheTest`, `CacheConfigurationTest` 로 local Spring cache 기록과 Redis cache manager 생성을 회귀 테스트로 고정했다.
- 루트 `README.md`, `BACKEND.md`, `docs/design-docs/backend-design/01-architecture-foundations.md` 에 "로컬 캐시는 Spring Cache, 분산 캐시는 Redis" 기준을 남겨 이후 기능이 같은 방향으로 구현되도록 맞췄다.
- 실행 검증은 `./gradlew test --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedTest --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedCacheTest --tests coin.coinzzickmock.providers.infrastructure.config.CacheConfigurationTest --console=plain`, `./gradlew architectureLint --console=plain`, `./gradlew check --console=plain` 으로 완료했다.
- 변경 범위 수동 5각도 점검에서는 readability 92 / performance 91 / security 94 / test quality 93 / architecture 95 로 판단했고, unresolved finding 이 없어 final score 93 으로 기록한다.
- PR은 [#14 로컬 캐시는 Spring Cache로, 분산 캐시는 Redis로 정리한다](https://github.com/gudwns1812/coin-zzickmock/pull/14) 로 생성했다.

## 맥락과 길잡이

관련 문서:

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

관련 코드:

- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java)
- [backend/build.gradle](/Users/hj.park/projects/coin-zzickmock/backend/build.gradle)
- [backend/src/main/resources/application.yml](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/application.yml)

이 작업에서 말하는 local cache는 "같은 애플리케이션 인스턴스 안에서만 공유되는 메모리 캐시"다.
distributed cache는 "여러 애플리케이션 인스턴스가 같은 저장소를 공유하는 캐시"를 뜻하며, 이 저장소에서는 Redis를 표준 구현으로 잡는다.

## 작업 계획

먼저 `red` 단계에서 두 가지를 실패로 고정한다.
하나는 market realtime feed 가 Spring local cache에 최신 시세를 기록해야 한다는 테스트고, 다른 하나는 Redis 사용 설정을 켰을 때 distributed cache manager 빈이 생겨야 한다는 테스트다.
현재 구현은 둘 다 없으므로 테스트가 실패해야 한다.

그 다음 `green` 단계에서 `backend/build.gradle` 에 Spring Cache 와 Redis starter 를 추가하고, 애플리케이션에 caching 을 켠다.
`providers` 또는 cache configuration 위치에 local `CacheManager` 와 조건부 Redis `CacheManager` 를 만들고, market 기능에는 local cache wrapper 협력 객체를 둔다.
`MarketRealtimeFeed` 는 더 이상 최신 시세를 `ConcurrentHashMap` 에 직접 넣지 않고 이 wrapper 를 통해 읽고 쓴다.
subscriber registry 는 SSE 연결 수명 관리용이므로 캐시가 아니며 그대로 메모리 구조를 유지해도 된다.

마지막 `refactor` 단계에서는 cache name, key, TTL, property 이름을 한 곳으로 정리하고, 설정 파일에 기본값과 설명을 남긴다.

## 구체적인 단계

1. market realtime feed 가 local Spring cache 에 값을 기록하는 테스트를 추가한다.
2. Redis 사용 설정 시 distributed cache manager 빈이 생성되는 테스트를 추가한다.
3. 테스트를 실행해 현재 실패를 확인한다.
4. Spring Cache / Redis 의존성과 설정을 추가한다.
5. local cache wrapper 와 cache configuration 을 추가한다.
6. `MarketRealtimeFeed` 를 wrapper 기반으로 바꾸고 관련 테스트를 맞춘다.
7. `./gradlew test --tests ...`, `./gradlew architectureLint`, `./gradlew check` 를 실행한다.
8. 변경 범위만 대상으로 품질 검토를 수행한다.
9. 브랜치/커밋/PR 흐름으로 마감한다.

## 검증과 수용 기준

실행 명령:

- `cd backend && ./gradlew test --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedCacheTest --tests coin.coinzzickmock.providers.infrastructure.config.CacheConfigurationTest`
- `cd backend && ./gradlew architectureLint`
- `cd backend && ./gradlew check`

수용 기준:

- `MarketRealtimeFeed.refreshSupportedMarkets()` 후 local Spring cache 에 `BTCUSDT`, `ETHUSDT` 최신 시세가 기록된다.
- `MarketRealtimeFeed.getMarket()` 와 `getSupportedMarkets()` 는 직접 `ConcurrentHashMap` 필드가 아니라 Spring cache wrapper 를 통해 동작한다.
- `coin.cache.redis.enabled=true` 일 때 distributed cache manager 빈이 생성된다.
- Redis를 쓰지 않는 기본 설정에서는 애플리케이션이 기존처럼 로컬 환경에서 기동 가능하다.

## 반복 실행 가능성 및 복구

- local cache는 재기동 시 비워져도 된다.
- Redis cache manager 생성 테스트는 실제 Redis 서버 연결을 요구하지 않도록 bean 생성까지만 검증한다.
- 설정이 잘못되면 local cache manager 만 남긴 상태로도 애플리케이션이 계속 동작해야 한다.

## 산출물과 메모

- 예상 PR 제목:
  Spring Cache / Redis 캐시 전략 정리
- 변경 메모:
  이번 작업은 현재 `MarketRealtimeFeed` 의 직접 메모리 캐시를 Spring Cache 뒤로 옮기고, 분산 캐시 표준 구현으로 Redis 경로를 연다.
