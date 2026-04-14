# Backend Legacy Quality Report

## 문서 목적

이 문서는 `coin-zzickmock` 프로젝트의 `backend` 모듈 전체 코드를 대상으로 수행한 품질 리뷰 결과를 정리한 보고서다.
단순한 "코드 스타일" 평가가 아니라, 실제 유지보수성과 운영 리스크에 영향을 주는 항목을 중심으로 분석했다.

이번 문서는 다음 목적을 가진다.

- 현재 백엔드의 구조적 강점과 약점을 한 문서에서 파악한다.
- 품질 영역별 점수와 등급을 통해 우선순위를 명확히 한다.
- 리팩토링을 시작할 때 무엇부터 손대야 하는지 공통 기준을 만든다.
- 추후 개선 작업 전후를 비교할 수 있는 기준선 문서로 사용한다.

## 리뷰 범위

- 대상 모듈: `backend`
- 분석 기준 경로
  - `backend/src/main/java`
  - `backend/src/test/java`
  - `backend/src/main/resources`
  - `backend/build.gradle`
  - `backend/AGENTS.md`
- 포함 관점
  - 가독성
  - 성능
  - 보안
  - 테스트 품질
  - 아키텍처

## 리뷰 방법

이번 리뷰는 `backend/AGENTS.md`의 규칙을 기준으로 진행했다.
특히 아래 원칙을 중요하게 반영했다.

- `support`에는 도메인 비종속 공통 지원 기능만 둔다.
- `Service`는 얇게 유지하고, 세부 로직은 협력 객체로 분리한다.
- 응답은 `ApiResponse`, 예외는 `CoreException + ErrorType` 규약을 유지한다.
- DB 조회 시 성능 이슈와 인덱스 무력화를 주의한다.
- 구조 변경 시 테스트가 회귀를 보호해야 한다.

리뷰 절차는 다음과 같다.

1. `backend` 전체 패키지 구조와 진입점을 확인했다.
2. `./gradlew test`를 실행해 현재 테스트 상태를 검증했다.
3. 가독성, 성능, 보안, 테스트, 아키텍처를 독립적으로 검토했다.
4. 중복 이슈를 합치고, 품질 영역별 점수와 등급을 산정했다.

## 실행 및 확인 결과

- `backend` 모듈 테스트 실행 결과: `BUILD SUCCESSFUL`
- 전체 Java 파일 수: `113`
- 확인된 테스트 메서드 수: `81`

테스트가 통과한다는 점은 긍정적이다.
다만 테스트 통과는 현재 구조가 건강하다는 뜻은 아니며, 특히 보안과 성능 영역에서는 즉시 개선이 필요한 이슈가 확인되었다.

## 점수 기준

점수는 `100점 만점` 기준이다.

| 등급 | 점수 기준 | 의미 |
| --- | --- | --- |
| A | 90 이상 | 매우 안정적이며 즉시 큰 리스크가 거의 없음 |
| B | 80 이상 | 전반적으로 양호하나 특정 영역 보강 필요 |
| C | 70 이상 | 운영 가능 수준이나 구조적 약점이 보임 |
| D | 60 이상 | 리팩토링 우선순위가 명확히 필요한 상태 |
| F | 60 미만 | 즉시 대응해야 할 위험이 존재함 |

## 종합 결과

| 영역 | 점수 | 등급 | 한 줄 평가 |
| --- | --- | --- | --- |
| Security | 52 | F | 인증/노출/기본 설정 측면의 위험이 큼 |
| Performance | 64 | D | 데이터 규모가 커질수록 급격히 불리한 구현이 존재 |
| Architecture | 61 | D | 패키지 책임 경계가 일부 무너져 장기 유지보수에 불리 |
| Test Quality | 78 | C | 핵심 회귀 테스트는 있으나 블라인드 스팟이 있음 |
| Readability | 76 | C | 전반적으로 읽히지만 경계와 정책이 일부 혼재됨 |
| Overall | 66 | D | 지금도 동작은 하지만 구조적으로 손볼 지점이 분명함 |

## 전체 요약

현재 백엔드는 "기능은 동작하지만 운영 관점의 위험과 구조적 부채가 누적되기 시작한 상태"로 보는 것이 가장 정확하다.
특히 보안과 성능은 단순 코드 미학 문제가 아니라 실제 장애 또는 악용 가능성과 연결되는 항목이 확인되었다.

반면, 모든 것이 나쁜 상태는 아니다.
배치와 외부 연동의 상위 분리, 응답/예외의 공통화 방향, 얇은 서비스 지향, 주요 비즈니스 흐름에 대한 테스트 존재 등은 분명한 강점이다.
즉, 기반은 있으나 일부 경계가 무너지고 일부 엔드포인트가 빠르게 확장되면서 규칙이 흔들린 상태에 가깝다.

---

## 1. Security 분석

### 점수

- `52 / 100`
- 등급: `F`

### 핵심 결론

이번 리뷰에서 가장 위험한 영역은 보안이다.
특히 "관리 표면 과다 노출", "익명 쓰기 엔드포인트 허용", "위험한 기본 설정" 세 가지가 동시에 존재한다.

### 주요 문제 1: actuator 관리 표면이 과도하게 열려 있음

#### 근거

- [`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/config/SecurityConfig.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/config/SecurityConfig.java)

#### 상세 내용

`application.yml`에서 actuator 웹 노출이 `include: "*"`로 열려 있고, health detail 도 `always`로 설정되어 있다.
또한 management server 가 별도 포트 `9293`로 열려 있다.

보안 설정은 `/actuator/health/**`만 익명 허용으로 보이지만, 나머지 actuator 경로는 "인증된 사용자라면 누구나" 접근 가능한 형태다.
이 프로젝트는 회원 가입과 로그인 엔드포인트가 공개되어 있으므로, 일반 사용자 계정만 있어도 actuator 내부 정보에 접근할 수 있는 구조가 된다.

#### 왜 위험한가

- runtime 내부 상태 노출 가능
- beans, mappings, metrics, 환경 관련 정보 노출 가능
- health detail 로 내부 의존성 상태가 외부에 드러날 수 있음
- 침해 후 lateral movement 또는 시스템 정찰 단계에 매우 유리함

### 주요 문제 2: `/api/v2/stocks/**` 전체가 `permitAll()`

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/support/auth/config/SecurityConfig.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/config/SecurityConfig.java)
- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/StockApiController.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/StockApiController.java)
- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/dto/request/ActiveStockSetRequestDto.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/dto/request/ActiveStockSetRequestDto.java)
- [`backend/src/main/java/stock/coinzzickmock/storage/redis/stock/publisher/StockActiveSetPublisher.java`](../backend/src/main/java/stock/coinzzickmock/storage/redis/stock/publisher/StockActiveSetPublisher.java)

#### 상세 내용

주식 조회 엔드포인트를 공개하는 것 자체는 가능하다.
문제는 쓰기 성격의 엔드포인트까지 같은 규칙으로 묶어 둔 점이다.

현재 공개 상태인 쓰기 엔드포인트 예시는 다음과 같다.

- `POST /api/v2/stocks/search`
- `POST /api/v2/stocks/active-sets`

특히 `active-sets`는 입력 리스트 길이 제한이 없고, 전달된 값을 Redis stream 으로 바로 publish 한다.
즉, 인증 없이 임의의 대량 요청을 보내 downstream consumer 를 오염시키거나 stream 부하를 만들 수 있다.

#### 왜 위험한가

- 익명 사용자가 상태를 바꿀 수 있음
- 검색 인기 랭킹 왜곡 가능
- Redis stream 오염 가능
- 대형 payload 를 통한 자원 고갈 위험이 존재

### 주요 문제 3: 운영 기본값이 위험함

#### 근거

- [`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtCookieFactory.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtCookieFactory.java)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtTokenProvider.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtTokenProvider.java)

#### 상세 내용

기본 설정값에 다음 내용이 포함되어 있다.

- DB 기본 계정: `root/root`
- DB 연결: `useSSL=false`
- Redis 비밀번호 기본값: 빈 값
- JWT secret 기본값: 고정 문자열
- JWT cookie secure 기본값: `false`

개발 환경에서만 사용할 의도였더라도, 이런 값이 운영 기본값처럼 살아 있으면 배포 실수 시 위험이 매우 커진다.

#### 왜 위험한가

- 환경변수 누락 시 즉시 취약 구성으로 기동될 수 있음
- 네트워크 상 토큰 탈취 위험 증가
- 고정 secret 기반 위조 가능성 증가
- cookie secure 비활성화로 HTTPS 외 전송 리스크 증가

### 주요 문제 4: access token 에 개인정보가 과도하게 포함됨

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtTokenProvider.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtTokenProvider.java)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtAuthenticationFilter.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/security/JwtAuthenticationFilter.java)

#### 상세 내용

access token 에 `memberId` 외에도 이름, 이메일, 전화번호, 우편번호, 주소, 상세주소, 투자성향 점수가 포함된다.
그런데 실제 인증 필터는 이 중 `memberId`만 사용한다.

즉, 인증에 필요 없는 개인정보를 토큰에 실어 매 요청마다 왕복시키는 구조다.

#### 왜 위험한가

- 토큰 노출 시 피해 범위가 커짐
- 프록시/로그/브라우저 저장소/클라이언트 유출 시 개인정보 유출 위험 증가
- 인증 목적 대비 과도한 데이터 노출

### 추가 문제: 계정 중복 확인 API 가 계정 열거 수단이 될 수 있음

공개된 `duplicate` 엔드포인트는 존재 여부를 직접 노출한다.
위험도는 위 항목들보다 낮지만, 대규모 계정 수집이나 password spraying 의 준비 단계로 사용될 수 있다.

### 보안 강점

- 비밀번호는 BCrypt 로 해싱된다.
- auth cookie 는 `HttpOnly` 로 발급된다.
- refresh token version 관리가 있어 재발급/로그아웃 정책이 비교적 잘 잡혀 있다.
- `invest`, `withdraw` 같은 멤버 변경 흐름에는 본인 검증이 존재한다.
- 예외 응답이 스택트레이스를 그대로 외부에 노출하지 않는다.

### 보안 권장 우선순위

1. actuator 노출 범위 축소
2. `/api/v2/stocks/**` 중 쓰기 엔드포인트를 인증 또는 내부 호출 전용으로 분리
3. `active-sets` 요청 크기 제한 추가
4. 운영 기본값 제거 또는 fail-fast 구성
5. JWT claim 최소화

---

## 2. Performance 분석

### 점수

- `64 / 100`
- 등급: `D`

### 핵심 결론

현재 성능 문제는 "아직 터지지 않았을 수는 있지만, 데이터가 커질수록 반드시 비용이 커지는 구조"가 명확히 보인다는 점이다.
특히 카테고리 조회와 검색이 대표적이다.

### 주요 문제 1: 카테고리 페이지네이션이 메모리 기반임

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockLoader.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockLoader.java)
- [`backend/src/main/java/stock/coinzzickmock/storage/db/stock/repository/StockJpaRepository.java`](../backend/src/main/java/stock/coinzzickmock/storage/db/stock/repository/StockJpaRepository.java)

#### 상세 내용

`loadCategoryPage()`는 다음 흐름으로 동작한다.

1. category 에 속한 모든 종목을 DB 에서 전부 가져온다.
2. 모든 row 를 domain 으로 변환한다.
3. Java 메모리에서 거래대금 기준 정렬한다.
4. 필요한 페이지 구간만 잘라서 반환한다.

이 구조는 데이터가 적을 때는 간단해 보일 수 있다.
하지만 실제로는 "페이지 조회"가 아니라 "전체 조회 후 부분 반환"이다.

#### 왜 문제인가

- 카테고리 데이터가 커질수록 응답 시간이 선형 증가
- 깊은 페이지 요청도 전체 데이터 처리 비용을 그대로 부담
- DB 정렬/페이지네이션 장점을 전혀 활용하지 못함
- 애플리케이션 메모리와 CPU 낭비

### 주요 문제 2: 검색 쿼리가 인덱스를 타기 어려움

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/storage/db/stock/repository/StockJpaRepository.java`](../backend/src/main/java/stock/coinzzickmock/storage/db/stock/repository/StockJpaRepository.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockSearchHandler.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockSearchHandler.java)
- [`backend/AGENTS.md`](../backend/AGENTS.md)

#### 상세 내용

검색 구현은 다음과 같은 쿼리 형태를 사용한다.

- `stockCode LIKE '%query%'`
- `UPPER(name) LIKE CONCAT('%', UPPER(query), '%')`

이 형태는 일반적인 B-Tree 인덱스가 잘 동작하지 않는다.
특히 컬럼에 함수를 적용하는 방식은 `backend/AGENTS.md`의 금지 원칙과도 충돌한다.

#### 왜 문제인가

- 데이터 증가 시 full scan 성향이 강해짐
- 사용자-facing 검색이라 체감 성능 저하가 직접 발생
- 검색은 트래픽 상위 경로가 되기 쉬워 병목이 되기 쉬움

### 주요 문제 3: 검색 수 증가가 read-convert-write 전체 갱신 구조

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockCommandHandler.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockCommandHandler.java)

#### 상세 내용

검색 수 증가 처리는 다음 흐름이다.

1. stock row 조회
2. domain 변환
3. count 증가
4. 전체 entity 재조립
5. save

이 방식은 단순 증가 연산에 비해 지나치게 무겁다.
자주 호출되면 select + update 비용과 객체 생성 비용이 함께 커진다.

### 주요 문제 4: p6spy 가 일반 implementation dependency 로 포함

#### 근거

- [`backend/build.gradle`](../backend/build.gradle)

#### 상세 내용

`p6spy-spring-boot-starter`가 일반 `implementation` 으로 포함되어 있다.
별도 환경 제한이 확실하지 않으면 운영에서도 JDBC 프록시와 SQL 로깅 비용이 들어갈 수 있다.

### 주요 문제 5: 요청 경로의 INFO 로그

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/StockApiController.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/StockApiController.java)
- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java)
- [`backend/src/main/resources/logback-spring.xml`](../backend/src/main/resources/logback-spring.xml)

#### 상세 내용

조회성 엔드포인트에서 request-path INFO 로그를 남기고 있고, prod profile 에서는 console 과 file appender 를 함께 사용한다.
트래픽이 늘면 불필요한 I/O 비용과 로그 볼륨 증가로 이어질 수 있다.

### 성능 강점

- stock info, indices, popular 경로는 Redis 우선 조회 구조를 사용한다.
- holiday 상태 판정은 존재 여부 조회 기반으로 비교적 가볍게 구성되어 있다.
- 명확한 JPA N+1 문제는 이번 스냅샷에서 눈에 띄지 않았다.

### 성능 권장 우선순위

1. category 조회를 DB 정렬 + DB 페이지네이션으로 전환
2. 검색 전략을 prefix search, 정규화 컬럼, 전문 검색 등으로 재설계
3. search count 증가를 단건 increment 쿼리로 변경
4. p6spy 를 dev/test 한정으로 제한
5. 조회성 INFO 로그 최소화

---

## 3. Architecture 분석

### 점수

- `61 / 100`
- 등급: `D`

### 핵심 결론

최상위 패키지 구분은 존재하지만, 실제 책임 경계는 일부 영역에서 무너져 있다.
특히 `support/auth`, `core/application/member`, `storage/db/member` 사이의 역할이 섞이면서 장기 유지보수성이 떨어지고 있다.

### 주요 문제 1: `support/auth` 가 member 도메인 유스케이스를 품고 있음

#### 근거

- [`backend/AGENTS.md`](../backend/AGENTS.md)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthService.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthService.java)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/api/AuthController.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/api/AuthController.java)

#### 상세 내용

가이드상 `support`는 인증/인가, 공통 응답, 공통 예외, 공통 설정 같은 도메인 비종속 기능만 두는 것이 원칙이다.
그런데 현재 `AuthService`는 다음을 모두 담당한다.

- register
- login
- refresh
- logout
- withdraw
- updateInvest
- member repository 접근
- member entity 조립

즉, 인증 지원 계층이라기보다 member 도메인 유스케이스 일부를 품고 있다.

#### 왜 문제인가

- member/account 정책의 소유권이 불명확해짐
- support 와 core 가 서로 꼬이기 시작함
- 이후 기능 추가 시 경계가 더 흐려질 가능성이 큼

### 주요 문제 2: member 규칙이 domain 과 entity 에 분산됨

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/domain/member/Member.java`](../backend/src/main/java/stock/coinzzickmock/core/domain/member/Member.java)
- [`backend/src/main/java/stock/coinzzickmock/storage/db/member/entity/MemberEntity.java`](../backend/src/main/java/stock/coinzzickmock/storage/db/member/entity/MemberEntity.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/member/MemberInvestService.java`](../backend/src/main/java/stock/coinzzickmock/core/application/member/MemberInvestService.java)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthProcessor.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthProcessor.java)

#### 상세 내용

Member aggregate 의 규칙이 한 군데에 있지 않다.
어떤 규칙은 domain `Member`가 갖고, 어떤 규칙은 persistence `MemberEntity`가 직접 갖는다.

예를 들어 invest 또는 refresh version 관련 행위가 entity 에서 직접 수행되며, storage 계층 객체가 `CoreException` 같은 비즈니스 예외를 던지는 경로가 생긴다.

#### 왜 문제인가

- business rule 의 authoritative source 가 사라짐
- persistence 리팩토링이 business behavior 변경으로 이어질 수 있음
- 테스트 경계가 흐려짐

### 주요 문제 3: `core.application` 안에 인프라 의존이 깊게 들어와 있음

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockLoader.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockLoader.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/MarketLoader.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/MarketLoader.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockCommandHandler.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockCommandHandler.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/market/MarketStatusService.java`](../backend/src/main/java/stock/coinzzickmock/core/application/market/MarketStatusService.java)

#### 상세 내용

`core.application` 내부 객체들이 다음 기술 요소를 직접 안고 있다.

- `RedisTemplate`
- JPA repository
- storage entity
- redis dto
- external client

즉, use case 계층이 adapter layer 에 강하게 결합되어 있다.

#### 왜 문제인가

- storage/cache 교체 비용이 `core` 수정 비용으로 번짐
- 비즈니스 계층 재사용성이 낮아짐
- 책임 경계가 package 이름과 실제 구현 사이에서 불일치

### 아키텍처 강점

- `batch`, `extern`, `support.error`의 상위 분리는 비교적 자연스럽다.
- stock 흐름은 서비스가 대체로 얇고, controller 도 HTTP/DTO 변환에 가깝다.
- `ApiResponse`와 `CoreExceptionHandler`를 통한 공통화 방향은 좋다.

### 아키텍처 권장 우선순위

1. member/account 유스케이스를 `core.application.member`로 정리
2. `support/auth`는 인증 지원 책임만 남기도록 축소
3. storage entity 에 있는 비즈니스 행위 정리
4. `core.application`에서 기술 세부 구현 의존을 점진적으로 분리

---

## 4. Test Quality 분석

### 점수

- `78 / 100`
- 등급: `C`

### 핵심 결론

테스트는 생각보다 괜찮다.
특히 happy path 뿐 아니라 side effect 와 token rotation 까지 잡고 있는 점은 분명한 강점이다.
다만 "깨지면 아픈 계약" 중 일부가 비어 있다.

### 주요 문제 1: auth validation / missing-cookie 분기 테스트 부족

#### 근거

- [`backend/src/test/java/stock/coinzzickmock/extern/AuthControllerIntegrationTest.java`](../backend/src/test/java/stock/coinzzickmock/extern/AuthControllerIntegrationTest.java)
- [`backend/src/main/java/stock/coinzzickmock/support/auth/api/AuthController.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/api/AuthController.java)
- [`backend/src/main/java/stock/coinzzickmock/support/error/CoreExceptionHandler.java`](../backend/src/main/java/stock/coinzzickmock/support/error/CoreExceptionHandler.java)

#### 상세 내용

현재 auth 통합 테스트는 다음 성격을 잘 다룬다.

- 로그인 성공
- refresh rotation
- 재사용 refresh token 거절
- 인증 필요 흐름
- 일부 권한 거절

하지만 다음은 비어 있다.

- invalid register payload
- invalid login payload
- invalid invest payload
- refresh cookie 자체가 없는 경우

즉, validation 과 예외 응답 계약의 일부가 테스트로 보호되지 않는다.

### 주요 문제 2: realtime stock endpoint 회귀 테스트 부족

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java)
- [`backend/src/test/java/stock/coinzzickmock/extern/StockApiIntegrationTest.java`](../backend/src/test/java/stock/coinzzickmock/extern/StockApiIntegrationTest.java)

#### 상세 내용

다음 엔드포인트는 controller 에 존재하지만 통합 테스트 보호가 약하다.

- `GET /api/v2/stocks/indices/{market}`
- `GET /api/v2/stocks/popular`

DTO 매핑과 예외 응답 계약이 추후 깨져도 놓칠 수 있다.

### 주요 문제 3: 일부 단위 테스트의 negative path 부족

#### 근거

- [`backend/src/test/java/stock/coinzzickmock/core/application/member/MemberInvestServiceTest.java`](../backend/src/test/java/stock/coinzzickmock/core/application/member/MemberInvestServiceTest.java)

#### 상세 내용

`MemberInvestService`는 member 미존재 시 `MEMBER_NOT_FOUND`를 던지지만, 현재 테스트는 이 케이스를 보호하지 않는다.
단위 테스트가 이미 있는 만큼 negative path 를 한두 개 더 추가하는 비용 대비 효과가 크다.

### 테스트 강점

- `MarketStatusServiceTest`는 주말/휴장/준비 안 된 캘린더 등 핵심 분기를 잘 보호한다.
- `KrxHolidayClientTest`는 외부 연동 파싱과 실패 분기를 잘 다룬다.
- `StockApiIntegrationTest`는 단순 상태 코드가 아니라 DB side effect 와 Redis publish 여부까지 확인한다.
- `AuthControllerIntegrationTest`는 로그인-리프레시-로그아웃 흐름을 실제로 잘 보존한다.

### 테스트 권장 우선순위

1. auth validation / missing-cookie 테스트 추가
2. indices / popular endpoint 통합 테스트 추가
3. member negative path 단위 테스트 추가
4. 보안 정책 변경 시 접근 제어 테스트도 함께 보강

---

## 5. Readability 분석

### 점수

- `76 / 100`
- 등급: `C`

### 핵심 결론

전반적인 코드 가독성은 심각하게 나쁜 편은 아니다.
오히려 서비스가 얇고 DTO 변환 규칙이 일정해 읽기 쉬운 부분이 많다.
다만, 경계가 애매한 영역과 정책이 두 군데에 나뉜 부분이 독해 비용을 올린다.

### 주요 문제 1: auth 경계가 읽는 사람 기준으로 모호함

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthService.java`](../backend/src/main/java/stock/coinzzickmock/support/auth/application/AuthService.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/member/MemberInvestService.java`](../backend/src/main/java/stock/coinzzickmock/core/application/member/MemberInvestService.java)

#### 상세 내용

`AuthService`가 대부분의 member/auth 흐름을 들고 있으면서 invest 하나만 별도 서비스로 위임한다.
그런데 소유권 검증 규칙은 양쪽에 중복되어 있다.

읽는 사람 입장에서는 다음 질문이 생긴다.

- member 정책의 주인은 누구인가
- auth 서비스와 member 서비스의 경계는 어디인가
- 왜 invest 만 분리되었는가

즉, 코드가 길어서 어렵다기보다 "책임 경계가 설명되지 않아" 이해 비용이 높다.

### 주요 문제 2: 검색 정책이 두 군데에서 다르게 표현됨

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/StockService.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/StockService.java)
- [`backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockSearchHandler.java`](../backend/src/main/java/stock/coinzzickmock/core/application/stock/implement/StockSearchHandler.java)

#### 상세 내용

`StockService.searchStocks()`는 blank keyword 를 예외 처리한다.
반면 `StockSearchHandler.search()`는 blank keyword 일 때 인기 종목을 반환한다.

현재 실제 동작은 상위 서비스가 예외를 던지므로 handler 의 blank fallback 이 살아 있지 않다.
즉, 읽는 사람이 두 정책을 모두 읽고 나서야 실제 계약을 이해할 수 있다.

### 주요 문제 3: 혼동되는 메서드명과 잔여 테스트 파일

#### 근거

- [`backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java`](../backend/src/main/java/stock/coinzzickmock/core/api/stock/StockRealTimeController.java)
- [`backend/src/test/java/stock/coinzzickmock/test/test.java`](../backend/src/test/java/stock/coinzzickmock/test/test.java)

#### 상세 내용

`stockPrice`와 `getStockPrice`는 실제 반환하는 데이터 성격이 다르지만 이름이 매우 비슷하다.
또한 `test/test.java`는 목적이 드러나지 않는 전형적인 잔여 파일처럼 보인다.

### 가독성 강점

- stock/market 서비스는 대체로 얇게 유지된다.
- controller 에서 `from(...)` 기반 DTO 변환을 사용해 수동 조립 노이즈가 적다.
- batch 는 오케스트레이션 역할로 비교적 명확하게 분리되어 있다.

### 가독성 권장 우선순위

1. auth/member 경계 설명력이 드러나도록 서비스 재배치
2. search 정책 단일화
3. 혼동되는 메서드명 정리
4. 잔여 테스트 파일 정리

---

## 핵심 강점 정리

현재 코드베이스에는 다음 강점이 있다.

- 테스트가 완전히 비어 있지 않고, 핵심 시나리오를 꽤 현실적으로 보호한다.
- `ApiResponse`와 예외 핸들러 중심의 통일 방향이 잡혀 있다.
- batch 와 extern 의 상위 분리가 비교적 명확하다.
- stock 흐름에서 서비스는 꽤 얇고 controller 는 비교적 단순하다.
- Redis 우선 조회 같은 성능 친화적 시도도 일부 잘 되어 있다.

즉, 이 프로젝트는 "무질서한 초기 코드"보다는 "좋은 규칙이 있었지만 일부 영역에서 예외가 쌓인 코드"에 더 가깝다.

---

## 가장 먼저 해결해야 할 문제

### 1순위: 보안 노출 차단

- actuator 외부 노출 범위 축소
- `/api/v2/stocks/**` 공개 정책 재설계
- `active-sets` 인증/권한/크기 제한 추가
- 운영 기본값 제거

### 2순위: category / search 성능 구조 수정

- category 조회 DB 페이지네이션 전환
- 검색 쿼리 전략 재설계
- search count 증가 로직 경량화

### 3순위: auth/member 경계 재정리

- `support/auth`와 `core/application/member` 책임 재분배
- entity 와 domain 에 분산된 규칙 정리
- member 유스케이스의 소유권 명확화

### 4순위: 테스트 블라인드 스팟 보강

- auth validation
- refresh missing-cookie
- indices/popular endpoint
- access control 테스트

---

## 권장 개선 순서

1. 보안 설정을 먼저 수정한다.
2. 그 다음 공개 API 중 쓰기 엔드포인트의 인증 정책을 분리한다.
3. category/search 경로의 query shape 를 바로잡는다.
4. auth/member 구조를 리팩토링한다.
5. 빠진 회귀 테스트를 채운다.
6. 마지막으로 네이밍과 잔여 파일 같은 가독성 문제를 정리한다.

## 결론

현재 `backend` 모듈은 "기능 개발을 계속 이어갈 수는 있지만, 지금 손보지 않으면 운영 리스크와 구조적 부채가 빠르게 커질 상태"다.
가장 큰 위험은 보안이고, 그 다음은 성능과 아키텍처다.

좋은 점도 분명하다.
현재 구조는 완전히 무너진 상태가 아니며, 공통 응답/예외 방향, 서비스 얇게 유지하기, 외부 연동 분리, 핵심 흐름 테스트 같은 좋은 토대가 남아 있다.

따라서 이번 레거시 문서의 목적은 "전면 재작성"이 아니라, 현재 코드를 살리면서 우선순위 높은 위험을 먼저 제거하고, 그 위에서 구조를 다시 바로 세우는 데 있다.

이 문서를 기준선으로 삼아, 이후 개선 작업에서는 다음 두 가지를 같이 확인하는 것이 좋다.

- 실제 위험이 줄었는가
- `backend/AGENTS.md`의 규칙에 더 가까워졌는가
