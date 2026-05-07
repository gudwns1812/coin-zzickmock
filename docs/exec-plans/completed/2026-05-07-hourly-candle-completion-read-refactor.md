# 1시간 이상 캔들 완료 판정 및 조회 경로 리팩터링

이 계획서는 `docs/exec-plans/README.md`와 `docs/exec-plans/plan-template.md`의 구조를 따른다.

## 목적 / 큰 그림

`1h` 이상 캔들 조회가 느리거나 `1D`, `1W`에서 500이 발생하는 문제를 한 번에 정리한다. 핵심 방향은 `market_candles_1h`를 "완료된 1시간봉만 저장되는 테이블"로 만들고, REST 조회 시점에는 더 이상 `1m` 60개 존재 여부를 매번 검증하지 않는 것이다.

완료 후에는 `1h`, `4h`, `12h`, `1D`, `1W`, `1M` 조회가 저장된 completed `1h` DB row를 기준으로 동작해야 한다. `1D`, `1W` telemetry tag 문제와 UTC 변환 위험도 같은 작업 안에서 수정한다.

## 진행 현황

- [x] 계획 초안 작성
- [x] 사용자 승인
- [x] 구현
- [x] 테스트
- [x] review 스킬 기반 검토 확인
- [x] 작업 종료 처리(완료 판단 및 completed 이동)

## 놀라움과 발견

- `1D`, `1W`는 캔들 롤업 로직보다 먼저 telemetry tag validation에서 대문자 `D`, `W`가 거부되어 500이 날 수 있다.
- `1h` 조회는 단순 DB 조회가 아니라 후보 `1h` row마다 `market_candles_1m` coverage를 다시 확인하는 구조라 느려진다.
- 현재 `1h` 저장 경로는 source minute가 하나라도 있으면 rollup row를 저장할 수 있어 partial `1h` row가 DB에 남을 수 있다.
- DB 문서는 UTC `DATETIME(6)` 저장을 명시하지만, 일부 변환 코드가 `ZoneId.systemDefault()`를 쓰면 로컬 KST 실행에서 완료 판정이 흔들릴 수 있다.

## 의사결정 기록

- 결정: `market_candles_1h`는 completed-only invariant를 가진다.
- 이유: `1h` 이상 REST 조회에서 source `1m` coverage를 매번 검증하면 응답 시간이 DB row 수와 gap 분포에 따라 급격히 나빠진다.
- 거절: 조회 시점마다 `1m` 60개를 검증하는 현재 방식 유지. 읽기 경로가 너무 비싸고, 사용자가 말한 "저장할 때 검증해야 한다"는 방향과 맞지 않는다.
- 보강: `1D`, `1W` telemetry 500 수정과 UTC 변환 고정을 계획 범위에 명시적으로 포함한다.
- 피드백 반영: 제품 명세에서 `market_candles_1h`를 "1시간 단위 집계 후보"가 아니라 "REST-visible completed hourly candle table"로 명확히 고친다.
- 피드백 반영: `1D`, `1W`, `1M`의 completed boundary를 구현 추측에 맡기지 않고 UTC calendar bucket 기준으로 명세와 테스트에 고정한다.

## 결과 및 회고

- `market_candles_1h` 저장 경로를 60개 연속 `1m` 원본이 있을 때만 저장하도록 변경했다.
- incomplete hour rebuild는 partial `1h` row를 새로 저장하지 않는다.
- REST completed hourly read path는 더 이상 조회 시점에 `market_candles_1m` coverage를 다시 스캔하지 않는다.
- 기존 운영 `market_candles_1h` row나 schema는 migration으로 건드리지 않는다.
- market candle JDBC persistence는 UTC `LocalDateTime`을 `DATETIME(6)`에 직접 저장하고, 읽을 때도 UTC `Instant`로 복원한다.
- `historyFinalized` SSE는 completed `1h`가 보이는 경우 `1h`, `4h`, `12h`, `1D`, `1W`, `1M` 무효화 범위를 함께 알린다.
- `MetricTags` interval validator는 `MarketCandleInterval` enum 값 전체를 허용한다.
- CodeRabbit 재검토는 rate limit으로 중단되었고, 직접 리뷰로 전환해 최신 피드백을 반영했다. 직접 리뷰에서 운영 DB를 바꾸는 V26 migration/backup table 접근을 제거했다.
- 검증: `cd backend && ./gradlew test --tests '*MarketHistoryRecorderTest' --tests '*MarketHistoryRecorderTransactionTest' --tests '*MarketHistoryPersistenceRepositoryTest' --tests '*MetricTagsTest' --tests '*MarketCandleRealtimeSseBrokerTest' --console=plain`, `cd backend && ./gradlew architectureLint check --console=plain`, `git diff --check`, `npm run check:branch -- fix/hourly-candle-completion-read-refactor`.

## 범위

- 이번에 하는 것(in scope):
  - `1h` 저장 또는 재빌드 시점에 해당 hour의 `1m` open time이 정확히 60개 연속인지 검증한다.
  - 검증 실패 시 partial `1h` row를 저장하지 않는다.
  - `1h` REST 조회는 `market_candles_1h`를 직접 조회하고 read-time `1m` coverage scan을 제거한다.
  - `4h`, `12h`, `1D`, `1W`, `1M` REST 조회는 completed `1h` DB row를 기준으로 rollup한다.
  - 부족한 오래된 구간 보충 경로가 현재 진행 중 bucket을 다시 끼워 넣지 않도록 interval별 completed boundary를 사용한다.
  - `1D`, `1W`, `1M` completed boundary를 UTC calendar bucket으로 명확히 정의한다.
  - 제품 명세에서 `market_candles_1h`가 REST-visible completed hourly candle table임을 명확히 한다.
  - `historyFinalized` SSE의 affected interval을 `1h` 이상 파생 기간까지 포함하도록 조정한다.
  - `MetricTags` interval validation이 `MarketCandleInterval`의 모든 값, 특히 `1D`, `1W`를 허용하도록 고친다.
  - market candle persistence의 `Instant`/`DATETIME` 변환을 UTC 기준으로 고정한다.
- 이번에 하지 않는 것(out of scope):
  - `4h`, `1D` 같은 추가 물리 테이블 생성.
  - 프론트 차트 UI 재설계.
  - Redis/Bitget provider 구조 전면 재작성.
- 후속 작업(선택):
  - 운영 데이터에서 partial `market_candles_1h` row 비율을 읽기 전용 점검 쿼리로 확인한다.

## 요구 사항 요약

- 기능 요구 사항:
  - 사용자는 `1h`, `4h`, `12h`, `1D`, `1W`, `1M` 캔들을 안정적으로 조회할 수 있어야 한다.
  - `1h` row는 source `1m` 60개가 연속으로 존재할 때만 REST-visible completed candle이 된다.
  - `market_candles_1h`는 partial rollup 후보 저장소가 아니라 REST-visible completed hourly candle table이다.
  - `1h` 이상 파생 기간은 모두 completed `1h` DB row를 기준으로 한다.
  - `1D`는 UTC 일 경계 `[00:00, 다음 00:00)`, `1W`는 UTC ISO 주 경계 `[월요일 00:00, 다음 월요일 00:00)`, `1M`은 UTC 월 경계 `[1일 00:00, 다음 달 1일 00:00)`를 따른다.
  - `1D`, `1W`, `1M` REST candle은 해당 bucket close가 지났고, 그 bucket 안의 모든 completed `1h` row가 존재할 때만 completed로 반환된다.
  - `1D`, `1W` 요청은 telemetry tag 때문에 500이 나면 안 된다.
- 비기능 요구 사항:
  - `1h` 이상 조회는 read path에서 `market_candles_1m`을 후보별로 반복 조회하지 않는다.
  - UTC bucket 경계와 DB UTC 저장 계약을 유지한다.
  - 외부 Bitget fallback은 부족한 오래된 구간에만 제한한다.

## 맥락과 길잡이

- 관련 문서:
  - `BACKEND.md`
  - `docs/design-docs/backend-design/03-application-and-providers.md`
  - `docs/design-docs/backend-design/06-persistence-rules.md`
  - `docs/design-docs/backend-design/07-clean-code-responsibility.md`
  - `docs/design-docs/backend-design/08-external-integration-rules.md`
  - `docs/design-docs/backend-design/09-exception-rules.md`
  - `docs/product-specs/coin-futures-candle-timeframe-spec.md`
  - `docs/generated/db-schema.md`
  - `OBSERVABILITY.md`
  - `docs/release-docs/observability/backend-observability-signal-map.md`
- 관련 코드 경로:
  - `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/market/application/history/MarketPersistedCandleReader.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/market/application/history/MarketHistoricalCandleAppender.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/market/application/history/MarketCandleRollupProjector.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/market/application/repository/MarketHistoryRepository.java`
  - `backend/src/main/java/coin/coinzzickmock/feature/market/infrastructure/persistence/MarketHistoryPersistenceRepository.java`
  - `backend/src/main/java/coin/coinzzickmock/providers/infrastructure/MetricTags.java`
- 선행 조건:
  - 구현 전 existing `market_candles_1h` row를 자동 삭제하거나 side table로 복제하지 않는 방향을 테스트와 문서로 고정한다.

## 문서 원문 대조표

| 작업 영역 | 반드시 읽은 원문 문서 | 적용해야 하는 규칙 | 구현 선택 | 금지한 shortcut | 검증 방법 |
| --- | --- | --- | --- | --- | --- |
| backend application | `BACKEND.md`, `03-application-and-providers.md`, `07-clean-code-responsibility.md` | 조회 service는 orchestration에 집중하고 Reader/Projector/Appender/Telemetry 같은 목적형 협력 객체로 분리한다. | `CompletedHourlyCandleBuilder` 같은 application 협력 객체를 두고 저장 시 completion 판단을 맡긴다. | `GetMarketCandlesService`나 recorder에 coverage query와 rollup 세부사항을 계속 누적한다. | architecture lint, market application targeted tests |
| persistence/DB | `06-persistence-rules.md`, `docs/generated/db-schema.md` | 조회 query는 누락 데이터를 생성하지 않고, 스키마 변경 시 Flyway와 schema 문서를 함께 갱신한다. | read-time coverage scan을 제거하되, 이번 변경에서 운영 DB schema/data migration은 추가하지 않는다. | generic save/upsert로 partial row를 조용히 유지하거나 운영 row를 자동 삭제/복제한다. | repository tests, schema 문서 확인 |
| product candle contract | `coin-futures-candle-timeframe-spec.md` | REST `1h+`는 completed `1h` 입력만 사용하고 UTC bucket 경계를 따른다. `market_candles_1h`는 REST-visible completed hourly candle table로 정의한다. | `1h` 저장 시 60 contiguous minutes를 검증해 completed-only row만 남기고, 제품 명세에 completed boundary를 명시한다. | row count나 min/max만으로 완료 판정한다. `1D/1W/1M`을 임의 24h/7일/30일 sliding window로 처리한다. | 59분/gap/60분 rollup tests, `1D/1W/1M` UTC calendar boundary tests |
| external fallback | `08-external-integration-rules.md`, Bitget reference 문서 | 외부 연동 세부사항은 infrastructure/provider 경계에 둔다. | 부족한 오래된 구간만 Redis/Bitget supplement를 사용하고, 현재 partial bucket은 보충하지 않는다. | DB가 비었다는 이유로 현재 진행 중 bucket까지 provider fallback으로 채운다. | appender boundary tests |
| exception/telemetry | `09-exception-rules.md`, `OBSERVABILITY.md`, `backend-observability-signal-map.md` | telemetry는 Provider 경계 뒤에 두고 낮은 카디널리티 label만 사용한다. | `MetricTags` interval validator를 enum 지원값과 일치시킨다. | telemetry tag validation 실패가 API 500으로 전파되게 둔다. | `1D`, `1W` telemetry unit test, API smoke |
| UTC persistence | `docs/generated/db-schema.md`, `coin-futures-candle-timeframe-spec.md` | DB에는 UTC 값 자체를 `DATETIME(6)`에 저장하고 지역 시간 해석은 화면 표시 계층으로 제한한다. | `ZoneId.systemDefault()` 기반 변환을 `ZoneOffset.UTC` 기준으로 고정한다. | 로컬/운영 timezone 차이를 전제하고 그대로 둔다. | non-UTC default timezone test 또는 변환 unit test |

대조표 점검:

- [x] 작업 영역별 원문 문서를 실제로 읽었다.
- [x] 제품/DB 문서와 설계 문서의 책임 차이를 구분했다.
- [x] 주변 코드 패턴이 원문 문서와 충돌할 때 어느 쪽을 우선할지 명시했다.
- [x] 금지한 shortcut을 구현 단계와 리뷰 단계에서 다시 확인할 수 있다.

## 작업 계획

1. 회귀 테스트를 먼저 추가한다.
   - `1h` 저장: source minute 59개, 중간 gap, 정확히 60개 케이스.
   - `1h` 조회: `market_candles_1m` coverage query 없이 `market_candles_1h` range만 읽는 케이스.
   - `4h`, `12h`, `1D`, `1W`, `1M`: completed `1h` DB row만 입력으로 사용하는 케이스.
   - `1D`: UTC `[day 00:00, next day 00:00)` 안의 24개 completed hourly row가 모두 있을 때만 반환되는 케이스.
   - `1W`: UTC ISO week `[Monday 00:00, next Monday 00:00)` 안의 168개 completed hourly row가 모두 있을 때만 반환되는 케이스.
   - `1M`: UTC calendar month `[first day 00:00, next month first day 00:00)` 안의 모든 completed hourly row가 있을 때만 반환되는 케이스.
   - telemetry: `1D`, `1W`, `1M`, `4h` tag가 validation을 통과하는 케이스.
   - UTC 변환: system default timezone이 UTC가 아니어도 open/close time 변환이 흔들리지 않는 케이스.
2. `1h` 저장 경로를 리팩터링한다.
   - `MarketHistoryRecorder.rebuildHourlyCandle`에서 source minutes를 읽은 뒤 `CompletedHourlyCandleBuilder`가 exact 60 contiguous open times를 검증한다.
   - complete이면 `market_candles_1h`에 저장하고, incomplete이면 새 hourly row를 저장하지 않는다.
3. repository 계약을 정리한다.
   - `findLatestCompletedHourly...`, `findCompletedHourlyCandles`처럼 read-time `1m` scan을 유발하는 계약을 제거하거나 직접 hourly read로 대체한다.
4. `1h+` 조회 경로를 단순화한다.
   - `1h`는 저장된 hourly row 직접 조회.
   - `4h`, `12h`, `1D`, `1W`, `1M`은 completed hourly rows를 rollup.
   - interval별 최신 completed boundary를 사용해 현재 진행 중 bucket이 historical supplement로 섞이지 않게 한다.
   - `1D` 최신 completed boundary는 직전 UTC day close, `1W`는 직전 UTC ISO week close, `1M`은 직전 UTC month close로 계산하되, 해당 bucket의 모든 completed hourly input이 없으면 반환하지 않는다.
5. 운영 데이터 정리 전략은 이번 PR에서 구현하지 않는다.
   - Flyway data migration, backup table, audit table을 추가하지 않는다.
   - 실제 정리가 필요하면 status/soft-delete/audit/restore 정책을 별도 설계한 뒤 운영 판단으로 수행한다.
   - DB 계약 변경이나 cleanup 기준이 문서에 반영되어야 하면 별도 PR에서 `docs/generated/db-schema.md`와 제품 명세를 갱신한다.
6. 제품 명세와 DB 문서를 구현 계약에 맞게 갱신한다.
   - `coin-futures-candle-timeframe-spec.md`에서 `market_candles_1h`를 REST-visible completed hourly candle table로 명확히 표현한다.
   - `coin-futures-candle-timeframe-spec.md`에서 `1D`, `1W`, `1M` completed boundary를 UTC calendar bucket 기준으로 고정한다.
   - `docs/generated/db-schema.md`에서 `market_candles_1h` 목적이 completed hourly REST read 기준임을 반영한다.
7. `historyFinalized` invalidation 범위를 확장한다.
   - 닫힌 `1m` 저장이 새 `1h` completion으로 이어지는 경우 `1h`, `4h`, `12h`, `1D`, `1W`, `1M` query가 무효화되도록 한다.
8. telemetry validator를 수정한다.
   - `MetricTags` interval tag validation을 `MarketCandleInterval.value()` 전체와 일치시킨다.
   - `1D`, `1W`가 telemetry 때문에 500을 만들지 않도록 한다.
9. UTC 변환을 고정한다.
   - market candle persistence mapper/entity/repository의 `Instant` <-> `LocalDateTime` 변환을 `ZoneOffset.UTC`로 통일한다.

## 구체적인 단계

1. 관련 문서를 읽고 대조표를 최신화한다.
2. market history 저장/조회/telemetry targeted test를 추가한다.
3. `CompletedHourlyCandleBuilder`를 추가하고 repository 삭제 계약은 두지 않는다.
4. recorder 저장 경로를 complete-only로 바꾼다.
5. persisted reader와 appender boundary를 direct hourly DB 기준으로 바꾼다.
6. telemetry interval validation과 UTC 변환을 수정한다.
7. product/db/observability 문서를 갱신한다. 특히 `market_candles_1h`의 completed table 의미와 `1D/1W/1M` completed boundary를 제품 명세에 명시한다.
8. targeted test, `architectureLint`, `check`, endpoint smoke를 실행한다.

## 수용 기준(테스트 가능한 형태)

- source `1m`이 59개이거나 중간 gap이 있으면 새 `market_candles_1h` row가 저장되지 않고 기존 row도 자동 삭제되지 않는다.
- source `1m`이 정확히 60개 연속이면 `market_candles_1h` row가 저장된다.
- `1h` REST 조회는 `market_candles_1m` coverage check를 수행하지 않는다.
- `4h`, `12h`, `1D`, `1W`, `1M` REST 조회는 completed `market_candles_1h` row만 rollup한다.
- 제품 명세는 `market_candles_1h`를 REST-visible completed hourly candle table로 설명하며, partial 후보 저장소로 해석될 문구를 남기지 않는다.
- `1D`는 UTC 일 경계 기준 24개 completed hourly row가 모두 있을 때만 반환된다.
- `1W`는 UTC ISO 주 경계 기준 168개 completed hourly row가 모두 있을 때만 반환된다.
- `1M`은 UTC 월 경계 기준 해당 월의 모든 completed hourly row가 있을 때만 반환된다.
- `1D`, `1W` API 요청은 telemetry tag validation 때문에 500을 반환하지 않는다.
- non-UTC JVM default timezone에서도 market candle UTC open/close time이 바뀌지 않는다.
- 현재 진행 중 hour/day/week/month bucket이 historical supplement로 응답에 섞이지 않는다.

## 위험과 완화

- 위험 시나리오 1: 기존 운영 DB에 partial `market_candles_1h` row가 많아 direct read 전환 후 응답이 줄어든다.
  - 예방: 이번 PR은 운영 row 삭제/복제 없이 앞으로 저장되는 row의 complete-only 조건만 고정한다.
  - 완화: 읽기 전용 점검 쿼리로 영향 범위를 확인한 뒤 별도 cleanup/repair 설계를 진행한다.
  - 복구: source `1m`이 남아 있으면 명시적 rebuild job으로 completed row를 재생성한다.
- 위험 시나리오 2: `1h` 저장 시 complete-only 조건이 너무 엄격해 일시적인 provider 지연에서 차트가 비어 보인다.
  - 예방: pending retry와 finalized event 경로를 유지한다.
  - 완화: REST는 completed history만, SSE는 live display를 담당하도록 역할을 분리한다.
  - 복구: 누락 minute backfill 후 hourly rebuild 재실행.
- 위험 시나리오 3: telemetry fix가 tag cardinality 규칙을 느슨하게 만든다.
  - 예방: 허용값을 자유 문자열이 아니라 `MarketCandleInterval` 지원값으로 제한한다.
  - 완화: invalid interval은 API validation에서 먼저 거절한다.
  - 복구: metric tag validator test로 회귀 차단.

## 검증 절차

- 실행 명령:
  - `cd backend && ./gradlew test --tests '*Market*History*' --console=plain`
  - `cd backend && ./gradlew architectureLint --console=plain`
  - `cd backend && ./gradlew check --console=plain`
  - smoke:
    - `/api/futures/markets/{symbol}/candles?interval=1h`
    - `/api/futures/markets/{symbol}/candles?interval=4h`
    - `/api/futures/markets/{symbol}/candles?interval=1D`
    - `/api/futures/markets/{symbol}/candles?interval=1W`
    - `/api/futures/markets/{symbol}/candles?interval=1M`
- 기대 결과:
  - targeted tests와 전체 backend check가 통과한다.
  - `1D`, `1W`는 HTTP 500 대신 정상 응답 또는 데이터 부재 시 정상 empty 응답을 낸다.
  - `1h`, `4h` 응답 지연이 read-time minute coverage scan에 비례하지 않는다.
- 실패 시 확인할 것:
  - stale partial `market_candles_1h` row 존재 여부.
  - historical supplement boundary가 현재 partial bucket을 열고 있는지.
  - telemetry tag validator가 interval enum과 어긋났는지.
  - JVM default timezone이 UTC가 아닐 때 변환 test가 흔들리는지.

## 반복 실행 가능성 및 복구

- 반복 실행 시 안전성:
  - completed-only rebuild는 같은 `(symbol, hourOpen)`에 대해 idempotent해야 한다.
- 위험한 단계:
  - 이번 PR에는 운영 데이터 cleanup migration이 없다.
- 롤백 또는 재시도 방법:
  - source `1m`이 남아 있으면 rebuild로 `1h` row를 재생성할 수 있어야 한다.

## 산출물과 메모

- 관련 로그:
  - 이전 분석에서 운영 API `1D`, `1W` HTTP 500을 확인했다.
  - 이전 분석에서 `1h`, `4h`가 성공하더라도 약 8초 지연될 수 있음을 확인했다.
- 남은 TODO:
  - 없음.
