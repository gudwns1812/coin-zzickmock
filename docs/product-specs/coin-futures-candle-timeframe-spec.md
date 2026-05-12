# 코인 선물 캔들 기간 지원 명세

## 목적

이 문서는 코인 선물 상세 화면의 과거 가격 차트가 어떤 기간 전환을 제공해야 하는지, 그리고 현재 DB 저장 구조가 그 요구를 어떻게 뒷받침해야 하는지를 정리한다.

현재 저장소의 시장 히스토리 스키마는 `market_candles_1m`, `market_candles_1h` 두 테이블만 가진다.
하지만 사용자 경험 관점에서는 그것만으로 충분하지 않다.
사용자는 차트에서 아래 기간을 바로 전환할 수 있어야 한다.

- `1m`
- `3m`
- `5m`
- `15m`
- `1h`
- `4h`
- `12h`
- `1D`
- `1W`
- `1M`

이 문서는 "DB에 어떤 물리 테이블이 지금 존재하는가"와 "사용자에게 어떤 기간을 보여줘야 하는가"를 구분해서 기록한다.

## 저장/제공 기준

- DB에 물리 저장되는 캔들 테이블은 현재 `1m`, `1h` 두 종류뿐이다.
- `1m`는 원본 분봉 저장 기준이다.
- `1h`는 조회 비용을 줄이기 위한 REST-visible completed 시간봉 롤업 저장 기준이다.
- API와 프론트 차트는 `1m`, `3m`, `5m`, `15m`, `1h`, `4h`, `12h`, `1D`, `1W`, `1M`을 지원 기간으로 고정한다.
- `3m`, `5m`, `15m`, `4h`, `12h`, `1D`, `1W`, `1M`는 별도 물리 테이블을 기본값으로 두지 않고 저장된 `1m`/`1h` 캔들에서 파생한다.

즉, 저장 구조는 "표시 가능한 전체 기간 목록"이 아니라 "파생 기간을 계산하기 위한 최소 저장 기준"이다.

## 제품 요구사항

### 사용자 노출 기간

심볼 상세 화면 `/markets/[symbol]`의 차트는 아래 기간 버튼을 항상 같은 순서로 제공해야 한다.

- `1m`
- `3m`
- `5m`
- `15m`
- `1h`
- `4h`
- `12h`
- `1D`
- `1W`
- `1M`

기간 버튼은 단순 장식이 아니라 실제 과거 가격 데이터를 바꿔 보여주는 동작이어야 한다.
버튼만 있고 데이터가 비어 있거나, 일부 기간만 더미 데이터로 남아 있으면 요구를 충족하지 못한 것으로 본다.

### 기간 의미

- `1m`, `3m`, `5m`, `15m`는 분 단위 캔들이다.
- `1h`, `4h`, `12h`는 시간 단위 캔들이다.
- `1D`, `1W`, `1M`은 일/주/월 경계를 따르는 캔들이다.
- 서버 저장, 조회, 롤업 기간 경계는 UTC를 기준으로 한다.
- 차트에 보이는 시각 라벨만 UTC 값을 UTC+9(KST)로 변환해 표시한다.
- `1W`, `1M`은 임의의 `7일`, `30일` 묶음이 아니라 달력 경계에 맞는 주간봉, 월간봉이어야 한다.

## 저장 및 제공 원칙

### 현재 저장 기준

- `1m`는 원본 저장 기준으로 유지한다.
- `1h`는 서버나 배치가 재사용할 수 있는 대표 롤업 저장 기준으로 유지한다.
- 서버의 분봉 저장 트리거, 시간봉 재빌드, 파생 기간 롤업은 UTC 경계를 기준으로 계산한다.
- DB에는 Java `Instant` 값을 UTC 기준의 `DATETIME(6)` 값으로 저장하고, 지역 시간 해석은 화면 표시 계층으로 제한한다.
- `market_candles_1m`에 저장되는 원본 분봉은 프론트엔드의 live price 합성 결과가 아니라, 백엔드 수집 경로의 거래소 1분봉 데이터를 기준으로 한다.
- 매초 market refresh 경로는 실시간 가격 캐시, SSE, 주문 체결, 청산 판단을 위한 ticker snapshot 갱신만 담당한다.
- 매 분 `1`초에 발행되는 `MarketMinuteClosedEvent`는 UTC 분 경계가 관측되었음을 알리는 내부 신호다. 닫힌 1분봉의 REST/DB 저장은 이 경계 신호 직후 바로 수행하지 않고, 기본 `coin.market.closed-minute-persistence-delay-ms=2500` 안정화 지연 뒤 거래소 candle을 수집해 처리한다.
- 이 안정화 지연 동안 candle SSE는 WebSocket에서 받은 최신/닫힌 버킷을 표시용 provisional candle로 계속 보여줄 수 있다. 이 provisional candle은 화면 연속성을 위한 overlay이며, DB/REST 과거 캔들의 권위 있는 원천으로 사용하지 않는다.
- 거래소 candle 수집 또는 저장이 일시 실패하면 우선 Spring Retry 기반의 짧은 즉시 재시도를 수행한다.
- 즉시 재시도를 모두 소진하면 `market_history_repair_events`에 durable repair event를 남기고, 커밋 이후 Redis List에 event id를 LPUSH해 repair worker를 깨운다.
- Redis List는 wakeup 신호만 담당하며, repair 대상의 권위 있는 상태와 중복 방지는 DB repair event의 `(symbol, interval, open_time)` identity와 상태 전이로 판단한다.
- 현재 1차 구현은 Redis LPUSH 자체가 재시도를 모두 소진한 경우 별도 DB rescan/sweeper로 복구하지 않는다. 운영에서 이 손실 경로를 닫아야 하면 sweeper 또는 outbox를 별도 릴리즈 범위로 추가한다.
- Spring Retry 성공 경로, repair worker 경로, 또는 최초 저장 경로에서 닫힌 1분봉이 실제로 DB에 저장되면, 커밋 이후 `historyFinalized` candle SSE 메시지를 발행해 프론트엔드가 관련 REST history query를 즉시 무효화할 수 있어야 한다. `historyFinalized`는 분 경계 scheduler tick이 아니라 DB persistence 완료를 의미한다.
- 이미 처리한 같은 심볼/분봉을 idempotent하게 다시 만난 경우에는 retry 상태를 해소할 수 있지만, 새로 저장된 history처럼 `historyFinalized` 메시지를 다시 발행하지 않는다.
- 저장 시 `volume`, `quote_volume`은 거래소 candle 응답 값을 함께 반영한다.
- 프론트엔드는 아직 닫히지 않은 live candle을 market-summary 최신가로 직접 합성하지 않는다.
- 프론트엔드는 백엔드의 candle SSE를 구독해 현재 버킷의 표시용 live candle만 덮어쓴다. 이 값은 DB 저장 원천으로 사용하지 않는다.

### 실시간 캔들 표시 기준

- 백엔드는 Bitget WebSocket candle 소스에서 `1m`, `1h` 기준 live candle을 수신한다.
- `1m`, `3m`, `5m`, `15m` live candle은 `1m` WebSocket candle을 기준으로 제공한다.
- `1h`, `4h`, `12h`, `1D`, `1W`, `1M` live candle은 `1h` WebSocket candle을 기준으로 제공한다.
- `3m`, `5m`, `15m`, `4h`, `12h`, `1D`, `1W`, `1M`은 프론트엔드가 직접 집계하지 않고 백엔드가 UTC 경계 기준으로 집계해 SSE로 보낸다.
- 프론트엔드는 REST candle 조회 결과를 닫힌 과거 캔들의 기준으로 사용하고, candle SSE 이벤트는 같은 `openTime` 버킷을 교체하거나 최신 버킷을 추가하는 화면 표시 레이어로만 사용한다. DB 저장 지연 중 REST 히스토리에 아직 없는 최신 버킷도 SSE 값으로 계속 표시할 수 있어야 한다.
- candle SSE stream은 기존 live candle payload와 별도로 `type: "historyFinalized"` 기본 메시지를 보낼 수 있다. 프론트엔드는 메시지의 `symbol`과 `affectedIntervals`가 현재 선택 상태와 일치할 때만 REST candle query를 invalidate한다.
- `historyFinalized` 이전의 SSE candle은 프로비저널 화면 값으로 취급하고, 최종값은 `historyFinalized` 후 REST 조회 갱신으로 반영한다.
- candle SSE가 아직 열리지 않았거나 끊긴 경우 프론트엔드는 기존 REST candle history와 market-summary 최신가 라인 fallback을 유지한다. 이 fallback은 거래용 캔들 데이터가 아니라 히스토리 부재 표시용이다.
- 브라우저 새로고침 또는 서버 재기동으로 in-memory live candle state가 비어 있으면, 첫 candle stream 구독 시 서버가 source interval을 lazy bootstrap할 수 있다.
- `1m`, `3m`, `5m`, `15m`의 live bootstrap source는 `1m`이며, `1h`, `4h`, `12h`, `1D`, `1W`, `1M`의 live bootstrap source는 live-only `1h`이다.
- hourly 계열 bootstrap은 가능한 경우 현재 hour의 `1m` candle들을 먼저 rollup하고, 그 값이 없을 때만 provider `1h` candle을 화면 표시용 fallback으로 사용한다.
- bootstrap으로 얻은 `1h`는 live display seed일 뿐이며, `market_candles_1h`에 저장하거나 REST history의 provider source로 사용하지 않는다.
- bootstrap은 같은 `(symbol, sourceInterval)` 요청을 coalescing하고 짧은 TTL로 반복 호출을 제한해야 하며, 이미 수락된 WebSocket candle을 덮어쓰면 안 된다. bootstrap이 먼저 들어온 경우에도 이후 WebSocket candle이 같은 key의 최신 상태를 이겨야 한다.
- 현재 live candle state와 SSE fan-out은 애플리케이션 메모리 기반 단일 인스턴스 상태다. 다중 인스턴스 운영으로 확장할 때는 공유 스트림/캐시 또는 sticky routing 정책을 별도로 결정해야 한다.

### 파생 기간 계산 기준

사용자에게 노출되는 기간은 아래 원칙으로 제공한다.

| 사용자 기간 | 우선 원본                               | 제공 방식      |
| ----------- | --------------------------------------- | -------------- |
| `1m`        | `market_candles_1m`                     | 직접 조회      |
| `3m`        | `market_candles_1m`                     | `1m` 묶음 롤업 |
| `5m`        | `market_candles_1m`                     | `1m` 묶음 롤업 |
| `15m`       | `market_candles_1m`                     | `1m` 묶음 롤업 |
| `1h`        | completed `market_candles_1h`           | 직접 조회      |
| `4h`        | completed `market_candles_1h`           | `1h` 묶음 롤업 |
| `12h`       | completed `market_candles_1h`           | `1h` 묶음 롤업 |
| `1D`        | completed `market_candles_1h`           | 일 경계 롤업   |
| `1W`        | completed `market_candles_1h`           | 주 경계 롤업   |
| `1M`        | completed `market_candles_1h`           | 월 경계 롤업   |

핵심은 "사용자 지원 기간 수"와 "DB 물리 테이블 수"를 1:1로 맞추지 않는 것이다.
현재 기준에서는 `1m`, `1h`만 물리 저장하고, 나머지는 애플리케이션 계층에서 파생하는 방식을 기본값으로 둔다.

REST history에서 `1h`는 `market_candles_1h`에 저장된 REST-visible completed row를 직접 조회한다.
`market_candles_1m` row는 저장 후 임의 delete/update가 없는 immutable source로 간주한다.
누락이나 손상은 REST query time마다 다시 탐지하지 않고, 저장 또는 재빌드 흐름에서만 탐지한다.
`market_candles_1h` row는 저장 또는 재빌드 시점에 해당 hour의 `[hourOpen, hourClose)` 구간에 기대되는 모든 `1m` open time 60개가 연속으로 존재할 때만 저장된다.
검증 알고리즘은 단순히 `COUNT = 60`만 보지 않고, `hourOpen + i * 1분` (`i=0..59`)의 60개 timestamp 각각에 해당하는 `market_candles_1m.open_time` 존재 여부를 명시적으로 확인한다.
중복 `open_time`은 `market_candles_1m(symbol_id, open_time)` unique key로 차단되어야 하지만, 구현은 그래도 60개 기대 timestamp를 순회하거나 동등한 set 비교로 검증해야 한다.
기대 open time 중 하나라도 없으면 gap으로 보고 해당 hour의 `market_candles_1h` 저장 또는 재빌드를 거부하여 partial row가 REST-visible completed row로 남지 않게 한다.
검증에 실패한 hour는 partial 후보 row를 REST-visible completed row로 남기지 않는다.
현재 schema에는 candle status나 soft-delete column이 없으므로 이 저장 경로는 기존 `market_candles_1h` row를 자동 삭제하거나 별도 backup/audit table을 생성하지 않는다.
검증 실패는 symbol id, hour open time, source count를 포함한 warning log로 남긴다.
MVP에서는 metric counter, alert, backfill enqueue를 추가하지 않고 log only로 둔다.
복구 가능성이 운영 요구가 되면 기존 row 삭제 방식이나 side table로 재사용하지 말고 `candle_status='stale'` 또는 `soft_deleted=true`, source-gap audit entry, purge grace period, admin restore/delete operation을 먼저 설계한 뒤 전환한다.
MVP의 concurrency pattern은 completed candle에 대해 source coverage guard를 통과한 row만 단일 `INSERT ... ON DUPLICATE KEY UPDATE`로 반영하는 upsert-only path다.
동일 symbol/hour의 동시 재빌드는 이 idempotent upsert-only semantics에 의존하며, stale row 정리는 별도 운영 판단 또는 명시적 재빌드/복구 흐름 안에서만 수행한다.
Future extension으로 DELETE+UPSERT pattern을 도입해야 하면 phantom과 concurrent insert를 막기 위해 해당 transaction은 `SERIALIZABLE`을 사용하거나 삭제 전 covered source rows와 target hour key를 명시적으로 key-locking한다.
deadlock, lock timeout, transient write failure가 발생하면 전체 hour rebuild transaction을 rollback하고 configurable default인 최대 3회, 초기 100ms, 최대 5s의 exponential backoff로 재시도한다.
reader는 이전 completed row 또는 새 completed row만 관측해야 한다.
중간 상태의 partial/stale row나 검증과 삭제 사이의 빈 상태를 REST completed candle로 노출하면 안 된다.
따라서 REST 조회 시점에는 후보 `1h` row마다 `market_candles_1m` coverage를 다시 스캔하지 않는다.
단순 row count, source minute min/max, 또는 첫/마지막 minute 존재만으로는 저장 시점의 completed 조건을 만족한 것으로 보지 않는다.
`3m`, `5m`, `15m`은 별도 저장 테이블이나 독립 completion 검사를 두지 않고, 저장 완료된 `1m` 이벤트를 기준으로 프론트엔드 REST history query를 무효화한다.
`4h`, `12h`, `1D`, `1W`, `1M`도 partial 후보가 아닌 completed `1h` 입력만 rollup해야 한다.
REST completed candle에서 `1D`는 UTC 일 경계 `[00:00, 다음 00:00)`, `1W`는 UTC ISO 주 경계 `[월요일 00:00, 다음 월요일 00:00)`, `1M`은 UTC 월 경계 `[1일 00:00, 다음 달 1일 00:00)` 안의 모든 completed `1h` row가 있을 때만 반환한다.
하나의 completed `1h` row라도 누락되면 상위 candle은 제외하며, 기대 hour 수는 calendar boundary로 계산한다.
MVP REST completed candle은 "부분 완성" threshold를 지원하지 않는다. 예를 들어 95% completeness나 `include_partial=true`는 completed candle 의미를 흐리므로 도입하지 않는다.
영구적인 `1m`/`1h` 데이터 손실은 REST query time이 아니라 저장 또는 재빌드 시점에 확인한다.
손실이 확인되면 우선 원천 데이터 backfill을 재시도하고, 복구할 수 없는 기간은 interpolation 없이 응답에서 제외하며 운영 공지나 관리자 메타데이터로 결손 구간을 노출한다.
월/주 단위 completeness 검사가 hot path 비용이 되면 `market_candles_1h(open_time, symbol_id)` 인덱스를 유지한 상태에서 materialized completeness table이나 hourly coverage bitmap 같은 계산된 metadata를 도입해 매 요청마다 전체 hourly row를 스캔하지 않게 한다.

### 후속 확장 원칙

특정 기간의 조회량이 커지거나 롤업 비용이 부담되면, 그때 `4h`, `1D` 같은 중간 테이블을 추가로 물리화할 수 있다.
하지만 첫 원칙은 "필요 이상으로 테이블을 늘리지 않는다"이다.

### 오래된 구간 조회 기준

DB에 저장된 `1m`, `1h` 캔들만으로 요청한 `limit`을 모두 채우지 못하면 서버는 부족한 오래된 구간만 캐시 경로에서 보충한다.
예를 들어 사용자가 `limit=200`을 요청했는데 DB가 80개만 반환할 수 있으면, 서버는 더 오래된 120개를 Redis/Bitget 경로에서 조회하고 DB 캔들과 합쳐 응답한다.

오래된 구간 보충은 현재 단계에서 DB에 저장하지 않는다.
direct `1h` REST history는 completed-hour reader가 제외한 현재 진행 중 hour를 캐시/Bitget 보충 경로로 다시 끼워 넣으면 안 된다.
DB completed 결과가 비어 있고 cursor도 없을 때 direct `1h` 보충 기준은 wall-clock `now`가 아니라 현재 닫힌 hour 경계여야 한다.
DB completed 결과가 일부 존재하면 보충은 가장 오래된 completed persisted candle보다 더 오래된 범위로만 제한한다.
Redis에는 `symbol + interval + aligned segment start + size200` 기준의 200캔들 세그먼트를 30분 동안 저장하고, exact `from/to/limit` 요청값을 캐시 키에 넣지 않는다.
`1D`, `1W`, `1M`처럼 `1h`에서 파생하는 기간은 최신 저장 `1h`가 아니라 최신 완료 일/주/월 버킷을 기준으로 DB 조회 범위를 잡는다.
따라서 현재 진행 중인 일/주/월 버킷이 미완성이라는 이유만으로 결과가 `limit - 1`개가 되어 Redis/Bitget 보충 경로가 열리면 안 된다.
Redis에 없을 때만 Bitget historical candle REST API를 호출하며, 반복 cache miss와 Bitget 호출량은 낮은 cardinality의 range bucket 단위로 관측한다.
Redis의 논리 세그먼트 크기는 200캔들이지만 Bitget `history-candles` 호출은 provider 제한에 맞춰 내부적으로 `startTime`~`endTime` 90일 이하의 하위 요청들로 나눈다.
Bitget 요청의 `startTime`과 `endTime`은 선택 기간 경계에 맞춰 정렬하고, 미래로 끝나는 논리 세그먼트는 현재 완료 경계까지만 provider에 요청한다.
`1D`, `1W`, `1M` provider 요청은 서버의 UTC 버킷 정책과 맞추기 위해 Bitget UTC granularity(`1Dutc`, `1Wutc`, `1Mutc`)를 사용한다.
이 전환으로 calendar interval Redis key의 granularity 구성요소도 `1D/1W/1M`에서 `1Dutc/1Wutc/1Mutc`로 바뀌며, 기존 key는 짧은 TTL이 만료되면 자연히 사라진다.
현재 또는 미래로 걸친 partial 세그먼트는 응답에는 사용할 수 있지만 완성된 200캔들 세그먼트처럼 Redis에 저장하지 않는다.
충분히 자주 요청되는 구간이 확인되면 별도 후속 결정으로 DB 승격을 검토한다.

## API 및 백엔드 요구사항

- 차트 조회 API는 기간 파라미터로 `1m/3m/5m/15m/1h/4h/12h/1D/1W/1M`만 허용해야 한다.
- 차트 조회 API는 선택적으로 `before` cursor를 받아 해당 시점 이전 캔들을 같은 interval 규칙으로 반환해야 한다.
- 허용되지 않은 기간 값은 명확한 오류로 거절해야 한다.
- 파생 기간 캔들은 `open`, `high`, `low`, `close`, `volume`를 모두 일관된 규칙으로 집계해야 한다.
- `open`은 묶음의 첫 캔들 시가, `close`는 마지막 캔들 종가를 사용해야 한다.
- `high`는 구간 최고가, `low`는 구간 최저가를 사용해야 한다.
- `volume`, `quote_volume`는 같은 구간 합계로 계산해야 한다.
- 기간 경계가 맞지 않는 부분 봉은 응답에 섞지 않는다. 예를 들어 `1W`를 요청했을 때 아직 닫히지 않은 미완성 주간봉 처리 정책은 별도로 고정해야 한다.
- REST `1h`와 그 이상 시간봉 파생 기간은 completed `1h` 입력만 사용해야 하며, 현재 진행 중인 partial hourly row를 응답에 포함하면 안 된다.
- candle SSE의 `historyFinalized` 메시지는 REST history가 갱신되었다는 신호이며, live candle payload와 같은 stream의 default JSON message로 전달된다.

## 프론트엔드 요구사항

- 기간 전환 버튼은 위의 10개 기간을 모두 보여줘야 한다.
- 버튼 순서와 라벨은 이 문서의 목록을 기준으로 고정한다.
- 기간을 바꾸면 캔들 데이터, 거래량 보조 표시, 차트 축 범위가 함께 갱신되어야 한다.
- candle SSE에서 `historyFinalized` 메시지를 받으면 현재 선택 심볼과 interval이 메시지의 `symbol`, `affectedIntervals`와 일치할 때만 candle history query를 무효화해야 한다.
- SSE 라이브 캔들은 `historyFinalized`가 도착하기 전에 화면 표시를 위해 즉시 갱신될 수 있으나, `historyFinalized`는 저장 완료 후에 발행되어 저장 반영의 정확성 경로가 된다.
- 기존 live candle bucket 전환 후 지연 refetch는 `historyFinalized`가 아직 도착하지 않은 경우의 fallback resilience 경로로만 유지한다. 이 refetch는 백엔드 기본 저장 지연 `2500ms`보다 짧게 실행되어 provisional bucket을 stale REST 응답으로 지우면 안 된다.
- 사용자가 차트를 왼쪽으로 이동해 현재 로드된 데이터의 시작점에 가까워지면 `before` cursor 기반 추가 조회를 수행해야 한다.
- 과거 데이터 추가 로드 후에도 사용자가 보고 있던 시각 범위를 최대한 유지해야 한다.
- 최신 시각 자동 복귀는 금지하고, 명시적 "최신 보기" 액션으로만 최신 위치로 이동해야 한다.
- 차트 좌상단에는 현재 심볼과 커서가 가리키는 캔들의 `O/H/L/C`를 항상 읽을 수 있어야 한다.
- 지원하지 않는 기간을 숨기기보다, 백엔드가 준비되기 전에는 명확한 disabled 상태나 준비 중 상태를 보여줘야 한다.

## 수용 기준

- 사용자는 `/markets/[symbol]` 상세 화면에서 `1m/3m/5m/15m/1h/4h/12h/1D/1W/1M`을 모두 선택할 수 있다.
- `1m`, `1h`는 DB에 저장된 원본 또는 completed 롤업 테이블에서 직접 조회된다.
- `1h` REST 조회는 저장 시점에 contiguous `1m` coverage가 증명된 completed hourly row만 반환하며 조회 시점에 `1m` coverage를 다시 스캔하지 않는다.
- `3m`, `5m`, `15m`, `4h`, `12h`, `1D`, `1W`, `1M`은 현재 저장 구조를 기준으로 올바르게 파생되어 응답된다.
- `4h`, `12h`, `1D`, `1W`, `1M`은 completed `1h` 입력만 rollup한다.
- 사용자가 과거 구간으로 이동하면 오래된 캔들이 연속적으로 이어 붙는다.
- 새로 저장된 닫힌 1분봉은 `historyFinalized` SSE를 통해 프론트엔드 query invalidation으로 연결된다.
- 새로고침 또는 서버 재기동 후에도 첫 candle stream 구독은 가능한 경우 source interval bootstrap을 통해 현재 live bucket을 복원한다.
- 실시간 가격 갱신만으로 차트 뷰가 강제로 최신 시각으로 튀지 않는다.
- 동일 시간 범위를 다른 기간으로 전환해도 OHLCV 집계 규칙이 일관된다.
- 주간봉과 월간봉은 임의 기간 합치기가 아니라 주간/월간 경계에 맞는 봉으로 보인다.

## 관련 문서

- [docs/product-specs/coin-futures-platform-mvp.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-platform-mvp.md)
- [docs/product-specs/coin-futures-screen-spec.md](/Users/hj.park/projects/coin-zzickmock/docs/product-specs/coin-futures-screen-spec.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
- [backend/storage/src/main/resources/db/migration/V3\_\_add_market_history_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/storage/src/main/resources/db/migration/V3__add_market_history_schema.sql)
