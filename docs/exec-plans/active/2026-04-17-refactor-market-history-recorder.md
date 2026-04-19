# MarketHistoryRecorder 리팩터링: recordSnapshot 메서드 분리 및 rollupHourly 개선

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
사용자가 별도 예외를 주지 않았다면, 이 계획서에 적는 예상 PR 제목과 실제 PR 본문도 모두 한국어로 작성한다. 코드, 명령어, 경로 같은 식별자만 원문 표기를 유지한다.

## 목적 / 큰 그림

현재 `MarketHistoryRecorder.java`의 `recordSnapshot` 메서드는 분 단위 캔들 기록과 시간 단위 캔들 기록 로직이 하나의 메서드에 섞여 있어 가독성이 떨어집니다. 또한
`rollupHourly` 메서드를 호출하는 과정이 부자연스럽다는 지적이 있었습니다.
이 작업을 통해 각 기록 로직을 명확한 책임을 가진 별도 메서드로 추출하고, 전체적인 코드 흐름을 더 자연스럽게 개선합니다.

## 진행 현황

- [x] (2026-04-17 22:15Z) 계획 초안 작성
- [x] (2026-04-17 22:19Z) 사용자 승인
- [x] (2026-04-17 22:21Z) 리팩터링 구현
- [x] (2026-04-17 22:22Z) 테스트 및 검증
- [ ] 품질 점수 확인 (QUALITY_SCORE.md 기준)
- [ ] PR 생성
- [ ] merge 후 completed 이동

## 놀라움과 발견

- 아직 없음

## 의사결정 기록

- 결정: `recordSnapshot` 내부 로직을 `recordMinuteCandle`과 `recordHourlyCandle`로 분리
  근거: 단일 책임 원칙(SRP)을 준수하고 가독성을 높이기 위함
  날짜/작성자: 2026-04-17 / Junie

- 결정: `rollupHourly` 내 다중 스트림 연산을 단일 루프로 변경
  근거: 불필요한 스트림 순회를 줄이고 성능을 개선하기 위함
  날짜/작성자: 2026-04-17 / Junie

## 결과 및 회고

- 아직 없음

## 맥락과 길잡이

- 관련 코드 경로: `backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java`
- 주요 클래스: `MarketHistoryRecorder`
- 주요 메서드: `recordSnapshot`, `rollupHourly`

## 작업 계획

1. `recordSnapshot` 메서드에서 분 단위 캔들 처리 로직을 `recordMinuteCandle` 메서드로 추출합니다.
2. `recordSnapshot` 메서드에서 시간 단위 캔들 처리 로직을 `recordHourlyCandle` 메서드로 추출합니다.
3. `recordHourlyCandle` 내부에서 `rollupHourly`를 호출하는 흐름을 더 자연스럽게 개선합니다.
4. 추출된 메서드들의 접근 제어자와 파라미터를 적절히 설정합니다.

## 구체적인 단계

1. `MarketHistoryRecorder.java` 파일을 엽니다.
2. `recordMinuteCandle(Long symbolId, double lastPrice, Instant observedAt)` 메서드를 생성하고 관련 로직을 이동합니다.
3. `recordHourlyCandle(Long symbolId, Instant observedAt)` 메서드를 생성하고 관련 로직을 이동합니다.
4. `recordSnapshot`에서는 위 두 메서드를 순서대로 호출하도록 변경합니다.
5. `rollupHourly` 메서드의 시그니처나 내부 로직 중 부자연스러운 부분이 있는지 다시 확인하고 조정합니다.

## 검증과 수용 기준

- 실행 명령: `./gradlew :backend:test --tests coin.coinzzickmock.feature.market.*` (관련 테스트 실행)
- 기대 결과: 모든 테스트가 통과해야 하며, 코드의 가독성이 이전보다 향상되어야 함.
- 실패 시 확인할 것: 메서드 추출 과정에서 파라미터 전달 오류나 트랜잭션 범위(현재 클래스 수준 또는 메서드 수준) 확인.

## 반복 실행 가능성 및 복구

- 반복 실행 시 안전성: 멱등성이 보장되는 로직이므로 반복 실행해도 안전함.
- 위험한 단계: 없음.
- 롤백 또는 재시도 방법: `git checkout`을 통해 이전 상태로 복구 가능.

## 산출물과 메모

- 예상 PR 제목: refactor: MarketHistoryRecorder 메서드 추출 및 가독성 개선
- PR 링크: TBD
- 관련 로그: TBD
- 남은 TODO: 실제 구현 및 테스트
