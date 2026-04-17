# 시장 과거 가격 DB 스키마 추가 계획

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다. 이 문서는 살아 있는 문서이며, `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고` 섹션을 작업 내내 최신 상태로 유지한다.

## 목적 / 큰 그림

이 작업의 목적은 코인 선물 시장 데이터의 기준 DB를 새로 추가하는 것이다. 완료 후에는 백엔드 DB에 거래 심볼 메타데이터, 1분봉 원본 데이터, 1시간봉 롤업 데이터를 저장할 수 있어야 하고, 1시간 롤업이 시간 구간 기준으로 자주 실행되어도 조회 비용이 과하게 커지지 않도록 시간 축 인덱스가 준비되어 있어야 한다.

이번 작업은 화면 API나 수집 배치를 바로 붙이는 단계는 아니고, 그 다음 단계들이 안전하게 올라갈 수 있도록 DB 스키마와 검증 기반을 먼저 만드는 단계다. 작업이 끝나면 H2 테스트 환경에서 새 테이블과 인덱스가 실제로 생성되는 것을 테스트로 확인하고, [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)에 같은 내용을 반영해야 한다.

## 진행 현황

- [x] (2026-04-16 21:15+09:00) 기존 DB/migration/test 패턴 조사 완료
- [x] (2026-04-16 21:20+09:00) 사용자 승인 수신: 시장 스키마와 시간 기준 인덱스 구현 진행
- [x] (2026-04-16 21:24+09:00) active 실행 계획 문서 생성 완료
- [x] (2026-04-16 21:28+09:00) 스키마 검증 테스트 추가
- [x] (2026-04-16 21:33+09:00) Flyway `V3` migration 추가 및 심볼 시드 반영
- [x] (2026-04-16 21:35+09:00) `db-schema.md` 동기화
- [x] (2026-04-16 21:36+09:00) `./gradlew test`, `./gradlew architectureLint`, `./gradlew check` 통과

## 놀라움과 발견

- 관찰:
  현재 DB 스키마 테스트 전용 케이스는 없고, H2 + Flyway가 `application-test.yml`에서 자동 적용되는 구조다.
  증거:
  [backend/src/test/resources/application-test.yml](/Users/hj.park/projects/coin-zzickmock/backend/src/test/resources/application-test.yml)에 `spring.flyway.locations: classpath:db/migration`가 설정되어 있다.

- 관찰:
  새 시장 스키마 migration을 처음 `V2`로 추가했더니, 저장소에 이미 `V2__add_member_credentials.sql`가 있어 Flyway가 애플리케이션 컨텍스트를 올리지 못했다.
  증거:
  `MarketHistorySchemaMigrationTest` 실패 로그에 `Found more than one migration with version 2`가 기록되었고, migration 번호를 `V3`로 올리자 테스트가 통과했다.

## 의사결정 기록

- 결정:
  코인 자산 마스터 테이블보다 거래 단위에 맞는 `market_symbols`를 기준 테이블로 둔다.
  근거:
  현재 프로젝트는 현물 코인 일반 정보보다 `BTCUSDT`, `ETHUSDT` 같은 선물 심볼과 차트 데이터가 핵심이며, 주문/포지션/차트도 모두 심볼 기준으로 작동한다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  차트 원본은 `1분봉`, 조회 최적화는 `1시간봉 롤업`으로 분리한다.
  근거:
  1시간봉 요청을 매번 1분봉 대량 집계로 처리하는 비용을 줄이면서도, 정합성의 기준은 가장 작은 공통 단위인 1분봉 하나로 유지하기 위함이다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  롤업 배치가 시간 범위 기준으로 자주 실행될 것을 고려해 `open_time` 선두 인덱스를 추가한다.
  근거:
  `(symbol_id, open_time)` 유니크 키는 심볼별 차트 조회에는 유리하지만, 여러 심볼 또는 시간 범위를 먼저 자르는 롤업 작업에는 `open_time` 선두 인덱스가 더 직접적이다.
  날짜/작성자:
  2026-04-16 / Codex

- 결정:
  시장 스키마 migration 번호는 `V2`가 아니라 `V3`를 사용한다.
  근거:
  저장소에 `V2__add_member_credentials.sql`가 이미 존재해 버전 충돌이 발생했기 때문이다.
  날짜/작성자:
  2026-04-16 / Codex

## 결과 및 회고

이번 작업으로 시장 데이터 영속성의 최소 뼈대를 추가했다.

- `market_symbols` 테이블을 추가하고 `BTCUSDT`, `ETHUSDT` 시드를 넣었다.
- `market_candles_1m`를 원본 1분봉 테이블로 추가했다.
- `market_candles_1h`를 1시간 롤업 테이블로 추가했다.
- 두 캔들 테이블 모두 `open_time` 선두 인덱스를 둬 시간 구간 기준 롤업/조회 비용을 낮췄다.
- H2 테스트에서 새 테이블과 인덱스가 실제로 생성되는 것을 확인했다.

남은 후속 작업은 두 가지다.

1. `feature.market`에 이 테이블을 읽고 쓰는 실제 persistence adapter와 수집 배치를 붙이는 일
2. 1시간 롤업을 어떤 스케줄과 보정 규칙으로 upsert할지 운영 정책을 정하는 일

## 맥락과 길잡이

이번 작업에서 먼저 읽을 파일은 아래와 같다.

- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)
- [backend/src/main/resources/db/migration/V1__initial_schema.sql](/Users/hj.park/projects/coin-zzickmock/backend/src/main/resources/db/migration/V1__initial_schema.sql)
- [backend/src/test/resources/application-test.yml](/Users/hj.park/projects/coin-zzickmock/backend/src/test/resources/application-test.yml)

이번 변경에서 추가할 스키마는 다음 세 가지다.

1. `market_symbols`
   차트와 주문이 참조할 거래 심볼 기준 정보
2. `market_candles_1m`
   원본 1분봉
3. `market_candles_1h`
   1시간 롤업 봉

여기서 중요한 점은 `USDT`, `perpetual` 같은 현재 고정된 값은 테이블 컬럼으로 늘리지 않는다는 것이다. 지금 단계에서 변하지 않는 값까지 DB에 넣으면 데이터가 아니라 설정을 저장하는 모양이 된다.

## 작업 계획

먼저 H2 테스트 환경에서 새 테이블과 인덱스가 반드시 존재해야 한다는 사실을 테스트로 고정한다. 이 테스트는 아직 `V2` migration이 없기 때문에 처음에는 실패해야 하고, 이후 migration을 추가하면 통과해야 한다.

그 다음 `backend/src/main/resources/db/migration/V3__add_market_history_schema.sql`을 추가한다. 이 migration에는 `market_symbols`, `market_candles_1m`, `market_candles_1h` 생성과 외래 키, 유니크 키, 시간축 인덱스를 포함한다.

마지막으로 [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)를 갱신해 새 시장 스키마를 source of truth 문맥에 연결한다.

## 구체적인 단계

1. `backend/src/test/java/...`에 마이그레이션 검증 테스트를 추가한다.
2. `backend/src/main/resources/db/migration/V3__add_market_history_schema.sql`을 추가한다.
3. `docs/generated/db-schema.md`에 테이블, 관계, 인덱스 목적을 반영한다.
4. `backend/`에서 아래 명령을 실행한다.
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew test --console=plain`
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew architectureLint --console=plain`
   `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew check --console=plain`

## 검증과 수용 기준

아래를 모두 만족하면 완료로 본다.

- H2 테스트 환경에서 `market_symbols`, `market_candles_1m`, `market_candles_1h`가 생성된다.
- `market_candles_1m`, `market_candles_1h`에는 `(symbol_id, open_time)` 유니크 키가 있다.
- 롤업 시간 범위 조회를 위한 `open_time` 선두 인덱스가 두 캔들 테이블에 존재한다.
- `db-schema.md`가 새 스키마를 설명한다.
- `./gradlew test`, `./gradlew architectureLint`, `./gradlew check`가 통과한다.

## 반복 실행 가능성 및 복구

Flyway migration은 누적 방식이므로 기존 `V1`을 수정하지 않고 새 `V2`만 추가한다. 테스트는 인메모리 H2에서 돌기 때문에 반복 실행해도 운영 데이터에 영향을 주지 않는다.

## 산출물과 메모

완료 후 아래 증거를 채운다.

- 새 migration 파일명:
  `backend/src/main/resources/db/migration/V3__add_market_history_schema.sql`
- 테스트 통과 핵심 줄:
  `BUILD SUCCESSFUL in 4s`
- architecture lint 통과 줄:
  `BUILD SUCCESSFUL in 3s`
- check 통과 줄:
  `BUILD SUCCESSFUL in 6s`

## 인터페이스와 의존성

이번 작업은 DB 스키마 계층 작업이므로 새 HTTP API나 application service 계약은 추가하지 않는다. 의존성은 아래 순서를 따른다.

- Flyway migration이 실제 DB 구조의 원문이다.
- `db-schema.md`는 migration 결과를 사람이 읽기 쉽게 요약한다.
- 테스트는 H2에서 Flyway 결과를 검증한다.

변경 메모:
2026-04-16 최초 작성. 시장 심볼 + 1분봉 원본 + 1시간봉 롤업 스키마와 시간축 인덱스 추가 작업을 위한 active 계획으로 시작한다.
2026-04-16 실행 반영. 기존 `V2` migration과 충돌하지 않도록 `V3`로 조정했고, H2 테스트와 Gradle 품질 게이트 통과 결과를 문서에 반영했다.
