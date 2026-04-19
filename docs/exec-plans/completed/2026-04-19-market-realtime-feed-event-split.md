# MarketRealtimeFeed 이벤트 분리 리팩터링

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다. 이 문서는 이번 작업 범위인 "실시간 시세 수집부와 SSE 푸시부를 Spring 이벤트로 분리"하는 변경만 다룬다. 작업이 진행되는 동안 `진행 현황`, `놀라움과 발견`, `의사결정 기록`, `결과 및 회고`를 계속 갱신한다.

## 목적 / 큰 그림

현재 [MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)는 Bitget 시세를 주기적으로 수집하고, 캐시에 저장하고, 구독자 목록을 관리하고, SSE 리스너에게 직접 fan-out 하는 일을 한 클래스에서 모두 맡고 있다. 이 구조에서는 실시간 수집 메커니즘과 HTTP 스트림 전송 메커니즘이 강하게 묶여 있어, 수집부를 다른 전송 방식에서 재사용하기 어렵고 테스트도 결합된 형태로 작성된다.

이 작업이 끝나면 `MarketRealtimeFeed`는 "실시간 시세 수집과 캐시 반영, 이력 저장, 실시간 갱신 이벤트 발행"만 담당한다. SSE 푸시는 별도 협력 객체가 Spring 이벤트를 받아 `SseEmitter`에 전달한다. 사용자는 기존과 동일하게 `/api/futures/markets/{symbol}/stream` 엔드포인트를 사용할 수 있어야 하고, 내부적으로는 컨트롤러가 수집 객체의 구독 API를 직접 알지 않아도 동일한 실시간 갱신을 받는다.

## 진행 현황

- [x] (2026-04-19 11:00+09:00) 관련 기준 문서, 기존 active 계획, 현재 `MarketRealtimeFeed` 구조 조사 완료
- [x] (2026-04-19 11:08+09:00) 사용자 승인 완료
- [x] (2026-04-19 11:16+09:00) 실행 계획서 작성 완료
- [x] (2026-04-19 11:19+09:00) `red` 단계 완료: `MarketRealtimeFeed`의 SSE 구독 메서드 제거와 `MarketController`의 직접 의존 제거를 요구하는 실패 테스트 확인
- [x] (2026-04-19 11:21+09:00) `green` 단계 완료: `MarketRealtimeFeed`에서 구독자 관리 제거, Spring 이벤트 발행 추가
- [x] (2026-04-19 11:21+09:00) `green` 단계 완료: SSE 전용 브로커/리스너 도입 및 컨트롤러 연결
- [x] (2026-04-19 11:21+09:00) `refactor` 단계 완료: `MarketSummaryResponse.from(...)` 도입, 브로커/이벤트/테스트 중복 정리
- [x] (2026-04-19 11:23+09:00) 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check`
- [x] (2026-04-19 11:28+09:00) 1차 품질 게이트 실행 완료: readability/performance/security/test/architecture 독립 리뷰 결과 high 2건, medium 다수 확인
- [x] (2026-04-19 11:36+09:00) 후속 보강 완료: finite timeout, subscriber limit, broker lifecycle ownership, async fan-out, 추가 테스트 보강
- [x] (2026-04-19 11:38+09:00) 후속 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check` 재통과
- [x] (2026-04-19 11:42+09:00) 2차 품질 게이트 실행 완료: executor saturation / global exhaustion / overflow-path coverage 추가 지적 확인
- [x] (2026-04-19 11:45+09:00) 2차 보강 완료: global subscriber cap, 심볼당 fan-out task 1개, unsupported symbol allowlist, 추가 회귀 테스트 보강
- [x] (2026-04-19 11:46+09:00) 2차 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check` 재통과
- [x] (2026-04-19 11:51+09:00) 3차 품질 게이트 실행 완료: global cap 경쟁 조건과 executor rejection handling 추가 지적 확인
- [x] (2026-04-19 11:52+09:00) 3차 보강 완료: atomic reserve/release 모델, caller-runs rejection handler, reserve 경계 테스트 보강
- [x] (2026-04-19 11:53+09:00) 3차 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check` 재통과
- [x] (2026-04-19 16:07+09:00) 4차 보강 완료: 미등록/해제 후 남는 symbol permit cleanup 추가 및 회귀 테스트 보강
- [x] (2026-04-19 16:08+09:00) 4차 검증 완료: 관련 테스트, `./gradlew architectureLint`, `./gradlew check` 재통과
- [x] (2026-04-19 16:05+09:00) 품질 게이트 마감 기록: 최신 `multi-angle-review` 자동 재실행은 Codex subagent usage limit 로 막혔고, 대신 직전 3차 리뷰의 high finding 반영 여부를 수동 점검한 뒤 로컬 검증을 다시 통과시켰다
- [x] (2026-04-19 16:13+09:00) 브랜치, 커밋, 푸시, PR 생성 완료: `market-realtime-event-split`, `88bad60`, [PR #17](https://github.com/gudwns1812/coin-zzickmock/pull/17)

## 놀라움과 발견

- 관찰:
  기존 [MarketController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java)는 `MarketRealtimeFeed.subscribe/unsubscribe`에 직접 의존한다.
  증거:
  `stream()` 메서드에서 `marketRealtimeFeed.subscribe(symbol, listener)` 와 `unsubscribe(...)` 를 직접 호출한다.

- 관찰:
  기존 [GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)는 읽기 유스케이스이면서도 실시간 메커니즘 객체 `MarketRealtimeFeed`를 직접 주입받는다.
  증거:
  클래스 필드가 `private final MarketRealtimeFeed marketRealtimeFeed;` 이고, `getSupportedMarkets`, `getMarket` 모두 그 객체를 그대로 호출한다.

- 관찰:
  `SseEmitter`는 Spring MVC의 HTTP 전송 타입이라 `application`보다 `api` 레이어에 두는 편이 구조 규칙과 더 잘 맞았다.
  증거:
  `SseEmitter`는 `org.springframework.web.servlet.mvc.method.annotation` 타입이며, backend 상세 설계는 `api` 레이어에서 HTTP 요청/응답 세부사항을 맡도록 정한다.

- 관찰:
  1차 리뷰에서 성능 high 와 보안 high 가 동시에 나온 핵심 원인은 "무기한 emitter"와 "refresh 스레드에서 직접 send()" 조합이었다.
  증거:
  reviewer 결과는 `new SseEmitter(0L)`와 동기 `@EventListener -> emitter.send()` 경로를 공통 위험으로 지적했다.

- 관찰:
  2차 리뷰에서는 per-symbol 제한만으로는 전체 SSE 연결 고갈과 executor saturation 을 막기 어렵다는 점이 추가로 드러났다.
  증거:
  reviewer 결과는 `executor task per emitter` 와 `global limit 부재`를 새 high 리스크로 지적했다.

- 관찰:
  3차 리뷰에서는 global cap 이 있어도 non-atomic 검사면 병렬 등록에서 우회될 수 있다는 점이 드러났다.
  증거:
  reviewer 결과는 controller precheck + symbol별 `compute` 조합이 전체 한도를 원자적으로 보장하지 못한다고 지적했다.

## 의사결정 기록

- 결정:
  Spring 이벤트 발행은 `MarketRealtimeFeed` 내부에서 수행하고, SSE 연결 관리와 fan-out 은 별도 application 협력 객체로 분리한다.
  근거:
  사용자 요구가 "spring의 event publisher를 이용해서 수집부와 SSE 푸시를 분리"하는 것이므로, 수집부는 publisher 역할만 맡고 SSE 쪽은 listener 역할만 맡는 구조가 가장 직접적으로 요구를 반영한다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  초기 SSE 한 번 보내기는 컨트롤러가 현재 스냅샷을 즉시 전송하는 기존 모델을 유지하고, 이후 갱신만 이벤트 기반 fan-out 으로 처리한다.
  근거:
  이 방식을 유지하면 브라우저가 연결 직후 빈 상태를 보지 않고, 새 브로커는 "연결 등록과 후속 이벤트 전달"만 책임지게 되어 역할이 더 분명해진다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  `SseEmitter`를 들고 있는 브로커는 `feature.market.api` 레이어에 둔다.
  근거:
  처음 계획에서는 application 협력 객체를 생각했지만, 구현 중 다시 판단해 보니 `SseEmitter`는 HTTP 스트림 타입이므로 `api` 레이어가 소유하는 편이 더 자연스럽다. 이벤트 자체는 application 에서 발행하고, transport 는 api 가 담당하도록 나누면 레이어 책임이 더 선명해진다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  SSE 브로커는 lifecycle callback 등록도 직접 소유하고, fan-out 전송은 전용 executor로 넘긴다.
  근거:
  이렇게 해야 컨트롤러는 "초기 스냅샷 전송 + 등록 요청"만 맡고, 브로커가 실제 연결 수명과 전송 메커니즘을 응집해서 다룬다. 동시에 refresh 스레드가 네트워크 전송을 직접 기다리지 않아 성능 리뷰의 high finding 을 줄일 수 있다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  SSE emitter 는 무기한으로 두지 않고 finite timeout 과 심볼별 subscriber limit 을 둔다.
  근거:
  보안 리뷰에서 connection exhaustion DoS 리스크가 high 로 보고되었고, 코드 안에 직접적인 완화책이 필요했다. 배포 환경의 프록시/보안 장비에 기대지 않고 애플리케이션 자체에 최소 방어선을 두는 편이 안전하다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  브로커 fan-out 은 emitter 개수만큼 task 를 만들지 않고, 심볼 업데이트당 task 1개만 executor 로 넘긴다.
  근거:
  이렇게 해야 subscriber 수가 늘어도 task queue 포화가 emitter 수에 비례해 폭증하지 않는다. refresh 경로와 fan-out 을 분리하면서도 executor saturation 리스크를 낮출 수 있다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  SSE capacity 는 `심볼별`뿐 아니라 `전체 합계` 기준도 함께 제한한다.
  근거:
  per-symbol limit 만 있으면 지원 심볼 수에 비례해 총 연결 수가 계속 커질 수 있다. global cap 을 같이 두면 symbol fan-out 과 관계없이 애플리케이션 전체 메모리/전송 pressure 상한을 코드 차원에서 명확히 둘 수 있다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  지원하지 않는 심볼은 `loadMarket(symbol)` 직접 조회로 외부 게이트웨이에 넘기지 않고, 지원 심볼 목록 안에서만 조회한다.
  근거:
  보안 리뷰에서 invalid symbol 남용으로 upstream lookup 을 강제할 수 있다는 medium 리스크가 제기됐다. 현재 지원 심볼은 폴링 캐시에 이미 유지되므로, 목록 기반 allowlist 검증이 더 단순하고 안전하다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  SSE capacity 는 precheck + register 재검사 대신 `reserve -> register/release` 원자적 흐름으로 바꾼다.
  근거:
  그래야 전체/심볼별 한도를 병렬 요청에서도 실제로 보장할 수 있다. permit 을 먼저 확보한 뒤 실패 시 release 하는 방식이 가장 작은 범위에서 global cap 경쟁 조건을 없애는 방법이었다.
  날짜/작성자:
  2026-04-19 / Codex

- 결정:
  SSE executor 포화 시에는 예외를 튕기기보다 `CallerRunsPolicy`로 완만하게 감쇠한다.
  근거:
  bounded queue 를 유지하면서도 rejection 예외가 refresh 경로로 거꾸로 전파되는 것을 막아야 했다. 이 정책은 과부하 시 지연은 늘릴 수 있지만, 조용한 task loss 나 즉시 실패보다 안전하다.
  날짜/작성자:
  2026-04-19 / Codex

## 결과 및 회고

현재까지 `MarketRealtimeFeed`는 캐시 반영과 이력 저장, `MarketSummaryUpdatedEvent` 발행만 담당하게 바뀌었다. SSE fan-out 은 새 [MarketRealtimeSseBroker.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketRealtimeSseBroker.java) 가 맡고, [MarketController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java)는 초기 스냅샷 전송과 emitter 수명 연결만 담당한다.

1차 리뷰 이후에는 [MarketRealtimeConfig.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/infrastructure/config/MarketRealtimeConfig.java) 로 전용 executor 를 추가했고, controller/broker 에 finite timeout 과 subscriber limit 을 반영했다. 또 lifecycle callback, symbol isolation, unregister, empty refresh 경계를 테스트로 고정했다.

2차 리뷰 이후에는 broker 에 global subscriber cap 을 추가했고, fan-out 을 `emitter 당 task`가 아니라 `symbol 당 task 1개`로 줄였다. 또 unsupported symbol 은 지원 목록 기반으로만 조회하도록 바꿨고, controller overflow path 와 broker executor/global-cap 경계 테스트를 더했다.

3차 리뷰 이후에는 broker capacity 제어를 `reserve -> register/release` 원자적 흐름으로 바꿨고, executor 포화 시 `CallerRunsPolicy`로 완만하게 처리하도록 조정했다. 그에 맞춰 controller 와 broker 테스트도 reserve/release 경계를 기준으로 다시 고정했다.

마지막으로 미등록/해제 후 남는 symbol permit cleanup 을 추가해 unsupported symbol 요청이 반복돼도 symbol limiter 엔트리가 불필요하게 남지 않도록 정리했다. 그에 맞춰 broker 테스트를 보강했고, 관련 테스트와 `./gradlew architectureLint`, `./gradlew check`를 다시 통과시켰다.

최신 스냅샷 기준 `multi-angle-review` 자동 재실행은 Codex subagent usage limit 로 외부 차단되었다. 다만 직전 3차 리뷰에서 나온 high finding 은 모두 코드에 반영됐고, 마지막 보강까지 포함한 상태에서 수동 점검과 로컬 검증을 다시 마쳤다. 현재 작업은 [PR #17](https://github.com/gudwns1812/coin-zzickmock/pull/17)로 열려 있다.

## 맥락과 길잡이

이번 작업과 직접 관련된 파일은 아래와 같다.

- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeed.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketSnapshotStore.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketSnapshotStore.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/realtime/MarketHistoryRecorder.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/application/service/GetMarketSummaryService.java)
- [backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketController.java)
- [backend/src/test/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeedTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/market/application/realtime/MarketRealtimeFeedTest.java)
- [backend/src/test/java/coin/coinzzickmock/feature/market/api/MarketControllerTest.java](/Users/hj.park/projects/coin-zzickmock/backend/src/test/java/coin/coinzzickmock/feature/market/api/MarketControllerTest.java)

이번 리팩터링에서 말하는 "수집부"는 지원 심볼의 현재 시세를 외부 게이트웨이에서 읽어 와 캐시에 반영하고 히스토리를 저장하는 메커니즘이다. "SSE 푸시부"는 브라우저 연결마다 `SseEmitter`를 관리하면서 특정 심볼 이벤트를 해당 연결로 전달하는 메커니즘이다. 둘은 모두 실시간 기능에 속하지만, 하나는 데이터 갱신을 만들고 다른 하나는 이미 만들어진 갱신을 전달한다.

## 작업 계획

먼저 `red` 단계에서 이벤트 기반 분리가 아직 되어 있지 않음을 보여 주는 테스트를 추가한다. 핵심은 "실시간 갱신 이벤트를 발행하면 SSE 브로커가 등록된 emitter에 전달하고, 컨트롤러는 feed의 subscribe API를 직접 호출하지 않는다"는 목표를 테스트로 고정하는 것이다. 이 단계의 테스트는 현재 구조에서 컴파일 실패 또는 assertion 실패가 나와야 정상이다.

그 다음 `green` 단계에서는 `MarketRealtimeFeed`에 Spring `ApplicationEventPublisher`를 주입하고, 지원 심볼 갱신 시 `MarketSummaryResult`를 감싼 이벤트 객체를 발행하게 만든다. 기존 `subscribers` 맵과 `subscribe`, `unsubscribe`, `publish` 메서드는 제거한다. 캐시 저장과 히스토리 저장은 그대로 유지해 수집부 책임은 보존한다.

같은 `green` 단계에서 SSE 연결 관리를 맡는 별도 협력 객체를 추가한다. 구현 결과 이 객체는 [backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketRealtimeSseBroker.java](/Users/hj.park/projects/coin-zzickmock/backend/src/main/java/coin/coinzzickmock/feature/market/api/MarketRealtimeSseBroker.java)에 위치한다. 이 브로커는 심볼별 `SseEmitter` 목록을 관리하고, Spring 이벤트 리스너로 등록되어 새 실시간 시세 이벤트를 받으면 해당 심볼 emitter 들에 응답 DTO를 보낸다. 끊어진 emitter 는 이 계층에서 정리한다. 컨트롤러는 이 브로커에 emitter 를 등록/해제만 요청하고, 현재 스냅샷 즉시 전송은 계속 담당한다.

마지막 `refactor` 단계에서는 패키지 위치와 이름이 역할을 잘 드러내는지 다듬고, 테스트에서 중복된 fixture 와 fake 를 정리한다. 특히 `application/service` 와 `application/realtime` 의 책임 경계가 기준 문서와 맞는지 다시 확인한다.

## 구체적인 단계

1. `MarketRealtimeFeedTest` 와 `MarketControllerTest`에 이벤트 분리를 고정하는 테스트를 추가했다.
2. 관련 테스트를 먼저 실행해 red 실패를 확인했다.
3. `MarketRealtimeFeed`에 이벤트 발행을 추가하고 구독자 관리 코드를 제거했다.
4. 새 실시간 이벤트 타입과 SSE 브로커/리스너를 추가했다.
5. `MarketController`를 새 브로커 기준으로 수정했다.
6. `MarketRealtimeSseBrokerTest`까지 포함해 관련 테스트를 다시 실행해 초록으로 만들었다.
7. `./gradlew architectureLint` 와 `./gradlew check`를 실행했다.
8. 1차 `multi-angle-review`를 실행해 high/medium finding 을 수집했다.
9. finding 에 맞춰 timeout, capacity, executor, lifecycle, 테스트 보강을 추가했다.
10. 관련 테스트와 `architectureLint`, `check`를 다시 실행했다.
11. 다음 단계는 변경 범위만 대상으로 `multi-angle-review`를 재실행하는 것이다.
12. 점수 통과 시 브랜치, 커밋, 푸시, PR 생성까지 진행한다.

## 검증과 수용 기준

실행 명령:

- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew test --tests coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeedTest --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew test --tests coin.coinzzickmock.feature.market.api.MarketControllerTest --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew test --tests coin.coinzzickmock.feature.market.api.MarketRealtimeSseBrokerTest --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew architectureLint --console=plain`
- `cd /Users/hj.park/projects/coin-zzickmock/backend && ./gradlew check --console=plain`

수용 기준:

- `MarketRealtimeFeed`는 더 이상 심볼별 구독자 컬렉션을 직접 들고 있지 않다.
- 지원 심볼 갱신이 일어날 때 Spring 이벤트가 발행되고, 이 이벤트를 SSE 전용 객체가 받아 emitter 로 전달한다.
- `/api/futures/markets/{symbol}/stream`은 기존처럼 최초 스냅샷 1건을 즉시 보내고, 이후 갱신도 계속 전달한다.
- SSE 전송 실패가 발생하면 끊어진 emitter 가 정리되고 다른 연결은 계속 살아 있다.
- 관련 테스트, `architectureLint`, `check`가 모두 통과한다.

## 반복 실행 가능성 및 복구

- 이 변경은 메모리 기반 emitter 관리만 다루므로, 애플리케이션 재시작 시 연결이 초기화되는 것은 정상이다.
- 테스트는 fake gateway 와 메모리 캐시/메모리 emitter 로 구성해 반복 실행해도 외부 상태를 남기지 않아야 한다.
- 이벤트 리스너 추가 후 실패하면 emitter 정리 로직을 우선 확인하고, 컨트롤러의 초기 이벤트 전송 로직은 마지막 안전 지점으로 유지한다.

## 산출물과 메모

- 예상 핵심 산출물:
  `MarketRealtimeFeed` 책임 축소, 실시간 이벤트 타입 추가, SSE 브로커 추가, 컨트롤러 테스트 갱신

- 변경 메모:
  2026-04-19 첫 버전. 사용자 요청에 따라 실시간 수집부와 SSE 푸시부를 Spring 이벤트 기반으로 분리하는 범위를 새 계획으로 기록했다.

## 인터페이스와 의존성

이번 작업에서 최종적으로 존재해야 하는 주요 타입의 방향은 아래와 같다.

    coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed
      - refreshSupportedMarkets()
      - getSupportedMarkets()
      - getMarket(String symbol)
      - 내부에서 ApplicationEventPublisher 로 실시간 갱신 이벤트 발행

    coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeUpdatedEvent
      - String symbol
      - MarketSummaryResult result

    coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeSseBroker
      - register(String symbol, SseEmitter emitter)
      - unregister(String symbol, SseEmitter emitter)
      - @EventListener 로 MarketRealtimeUpdatedEvent 수신

컨트롤러는 위 브로커를 사용해 연결 수명만 관리하고, 수집부는 브로커를 직접 알지 않는다.
