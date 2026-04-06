# 백엔드 레거시 리팩토링 분석

## 범위

이번 분석은 `backend/src/main/java`와 `backend/src/test/java`를 기준으로 진행했다.
일관성이 없는 부분은 모두 리팩토링 대상으로 포함했다.

## 현재 상태 요약

- 백엔드 테스트는 현재 기준으로 `./gradlew test`가 통과한다.
- 다만 테스트 통과와 별개로 응답 규약, 네이밍, 책임 분리, 타입 모델링, 예외 구조에서 일관성이 많이 깨져 있다.
- 특히 컨트롤러 응답 형식, 예외 처리 표준, 패키지 구조, 인증 흐름, 주식 도메인 모델의 타입 표현은 우선 정리 대상이다.

## 목표 아키텍처 방향

- `global` 패키지는 제거한다.
- 상위 패키지는 멀티 모듈 전환을 고려해 `core`, `storage`, `support`로 재편한다.
- 비즈니스 예외는 `CoreException` 하나로 통일하고, 실제 에러 정보는 `ErrorType`으로 관리한다.
- 예외 응답과 로깅 정책은 `ExceptionHandler`에서 일괄 처리한다.
- 공통 상수, 공통 응답, 공통 예외는 도메인 밖 아무 곳이나 두지 말고 목적이 드러나는 패키지로 이동한다.

## 우선순위 높은 리팩토링 대상

| 우선순위 | 대상 | 근거 파일 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 높음 | 예외 처리 표준화 | `backend/src/main/java/stock/mainserver/global/error/*.java`, `backend/src/main/java/stock/mainserver/auth/exception/*.java`, `backend/src/main/java/stock/mainserver/global/handler/StockGlobalExceptionHandler.java` | 예외 클래스가 여러 형태로 흩어져 있고, 상태 코드와 메시지 정책도 예외마다 분산되어 있다. `CoreException` + `ErrorType` 구조로 통일해야 예외 정책을 한 곳에서 제어할 수 있다. |
| 높음 | 상위 패키지 재설계 | `backend/src/main/java/stock/mainserver/**` 전체 | 현재는 `global`, `service`, `repository`, `auth`가 뒤섞여 있어 멀티 모듈로 분리하기 어렵다. `core`, `storage`, `support` 기준으로 재정렬해야 경계가 선명해진다. |
| 높음 | 응답 규약 통합 | `backend/src/main/java/stock/mainserver/global/response/SuccessResponse.java`, `backend/src/main/java/stock/mainserver/global/response/ErrorResponse.java`, `backend/src/main/java/stock/mainserver/controller/StockApiController.java`, `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java` | 성공 응답은 `Success`, 실패 응답은 `success`를 사용해 필드명이 다르다. 같은 서비스 안에서 응답 키 규약이 다르면 프론트 분기 처리와 문서화가 계속 꼬인다. |
| 높음 | 컨트롤러 역할 재분리 | `backend/src/main/java/stock/mainserver/controller/StockApiController.java`, `backend/src/main/java/stock/mainserver/controller/StockRealTimeController.java`, `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java` | 같은 `/api/v2/stocks` 아래에서 조회 성격이 다른 API가 두 컨트롤러로 나뉘어 있고, 컨트롤러가 응답 생성과 쿠키 헤더 조립까지 직접 담당한다. 엔드포인트가 늘수록 중복과 변경 비용이 커진다. |
| 높음 | 서비스 책임 분리 | `backend/src/main/java/stock/mainserver/service/StockService.java`, `backend/src/main/java/stock/mainserver/auth/service/AuthService.java`, `backend/src/main/java/stock/mainserver/service/init/HolidayService.java` | 한 서비스가 조회, 매핑, 캐시 접근, 페이징, 검색 fallback, 토큰 발급, 권한 검증, 외부 연동까지 여러 책임을 동시에 가진다. 테스트와 변경 영향 범위를 작게 유지하기 어렵다. |
| 높음 | 기간 데이터 계산 로직 정리 | `backend/src/main/java/stock/mainserver/dto/response/StockPeriodResponseDto.java` | `prevPrice`와 `openFromPrev`, `closeFromPrev`의 의미가 이름과 계산식에서 어긋난다. 값 의미가 흔들리면 차트, 수익률, 비교값 계산이 잘못 연결될 가능성이 높다. |
| 높음 | 숫자형 도메인 타입 재설계 | `backend/src/main/java/stock/mainserver/entity/Stock.java`, `backend/src/main/java/stock/mainserver/entity/StockHistory.java`, `backend/src/main/java/stock/mainserver/dto/redis/StockDto.java`, `backend/src/main/java/stock/mainserver/dto/response/*.java` | 가격, 거래량, 증감값을 대부분 `String`으로 다루고 있어 매번 파싱이 필요하다. 숫자 계산이 필요한 기능이 늘수록 예외 처리와 정렬 오류가 생기기 쉽다. |
| 높음 | 인증 흐름 공통화 | `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java`, `backend/src/main/java/stock/mainserver/auth/security/JwtAuthenticationFilter.java`, `backend/src/main/java/stock/mainserver/auth/security/JwtTokenProvider.java` | 쿠키 조회 로직이 중복되고 JWT 클레임 키 네이밍도 일관되지 않다. 인증 정책이 여러 곳에 흩어져 있으면 보안 수정 시 누락 위험이 커진다. |

## 일관성이 없는 부분과 리팩토링 이유

### 1. 예외 처리 구조가 일관되지 않음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 예외 클래스가 역할별로 흩어져 있음 | `backend/src/main/java/stock/mainserver/auth/exception/*.java`, `backend/src/main/java/stock/mainserver/global/error/*.java` | 인증 예외와 도메인 예외가 서로 다른 기준으로 분리되어 있다. | 비즈니스 예외는 `CoreException` 하나로 통일해야 정책을 예측 가능하게 만들 수 있다. |
| 예외마다 상태 코드 정책이 분산됨 | `backend/src/main/java/stock/mainserver/global/handler/StockGlobalExceptionHandler.java` | 핸들러에서 예외 타입별로 상태 코드를 직접 연결하고 있다. | 상태 코드와 메시지 정책은 `ErrorType`으로 올려야 수정 지점이 하나로 모인다. |
| 예외 클래스 생성자 규칙 불일치 | `backend/src/main/java/stock/mainserver/global/error/*.java`, `backend/src/main/java/stock/mainserver/auth/exception/*.java` | 어떤 예외는 기본 생성자가 있고, 어떤 예외는 메시지 생성자만 있다. | 예외 작성 규칙이 계속 흔들리면 코드 리뷰 기준도 불명확해진다. |
| 미사용 예외 존재 | `backend/src/main/java/stock/mainserver/global/error/RedisTypeException.java`, `backend/src/main/java/stock/mainserver/global/error/TokenFetchException.java` | 실제 사용 흔적이 없다. | `CoreException` 체계로 정리할 때 같이 정리하지 않으면 죽은 코드가 계속 남는다. |

### 2. 응답 객체 규약이 일관되지 않음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 성공/실패 응답 필드명 불일치 | `backend/src/main/java/stock/mainserver/global/response/SuccessResponse.java`, `backend/src/main/java/stock/mainserver/global/response/ErrorResponse.java` | 성공 응답은 `Success`, 실패 응답은 `success`를 사용한다. | 클라이언트에서 응답 해석 규칙이 둘로 갈라지고, 문서와 테스트도 이중 관리가 된다. |
| 실패를 성공 응답 객체로 표현 | `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java`, `backend/src/main/java/stock/mainserver/controller/StockApiController.java` | 중복 아이디 응답과 빈 검색어 응답에서 실패 의미를 `SuccessResponse`에 담고 있다. | HTTP 상태와 응답 바디 의미가 섞이면 예외 처리 정책을 공통화하기 어렵다. |
| 제네릭 타입 선언이 모호함 | `backend/src/main/java/stock/mainserver/controller/StockApiController.java`, `backend/src/main/java/stock/mainserver/controller/StockRealTimeController.java`, `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java` | 대부분 `ResponseEntity<?>`를 사용한다. | 컨트롤러 계약이 약해져서 응답 타입 추적과 문서 자동화 품질이 떨어진다. |

### 3. 네이밍 규칙이 일관되지 않음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 클래스명 규칙 위반 | `backend/src/main/java/stock/mainserver/global/scheduler/logWarm.java` | 클래스명이 `logWarm`으로 시작 소문자를 사용한다. | 자바 기본 규칙과 어긋나서 검색성과 가독성이 떨어진다. |
| 메서드명 규칙 위반 | `backend/src/main/java/stock/mainserver/controller/StockApiController.java`, `backend/src/main/java/stock/mainserver/controller/StockRealTimeController.java` | `StockCounter`, `StockPrice`처럼 메서드명이 대문자로 시작한다. | 코드 스타일이 파일마다 달라지면 리뷰와 리팩토링 비용이 커진다. |
| 필드명 규칙 혼재 | `backend/src/main/java/stock/mainserver/auth/dto/request/AddressRequest.java`, `backend/src/main/java/stock/mainserver/auth/entity/Member.java`, `backend/src/main/java/stock/mainserver/auth/security/JwtTokenProvider.java` | `zipcode`, `zipCode`, `Address`, `AddressDetail`처럼 같은 의미가 서로 다른 이름으로 표현된다. | 매핑 누락, 직렬화 실수, 프론트 계약 불일치가 발생하기 쉽다. |
| 의미 없는 이름 사용 | `backend/src/main/java/stock/mainserver/dto/data/Response.java`, `backend/src/main/java/stock/mainserver/dto/data/Body.java`, `backend/src/main/java/stock/mainserver/dto/data/Items.java` | 너무 일반적인 이름이라 역할이 드러나지 않는다. | 외부 연동 DTO가 늘어나면 충돌과 오독 가능성이 높아진다. |
| 사용하지 않는 요청 필드 존재 | `backend/src/main/java/stock/mainserver/auth/dto/request/RegisterRequest.java` | `fgOffset`가 실제 로직에서 사용되지 않는다. | API 계약에 불필요한 노이즈가 남고, 실제 필요한 값인지 계속 오해를 만든다. |

### 4. 패키지 구조가 멀티 모듈 전환에 불리함

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| `global` 패키지 의존 | `backend/src/main/java/stock/mainserver/global/**` | 설정, 응답, 예외, 핸들러, 스케줄러가 전부 `global` 아래 섞여 있다. | `global`은 무엇이든 들어갈 수 있는 이름이라 모듈 경계를 흐린다. |
| 계층 기준 패키지 분리 | `backend/src/main/java/stock/mainserver/controller`, `service`, `repository`, `dto`, `entity` | 기능보다는 기술 계층 기준으로 흩어져 있다. | 멀티 모듈 전환 시 어느 패키지가 어느 모듈로 가야 하는지 애매해진다. |
| 인증이 독립 영역으로 반쯤 분리됨 | `backend/src/main/java/stock/mainserver/auth/**` | `auth`는 따로 있지만 공통 응답, 예외, 핸들러는 바깥에 남아 있다. | 도메인과 지원 기능의 경계가 불완전해서 재배치 비용이 커진다. |
| 빈 패키지와 미완성 구조 존재 | `backend/src/main/java/stock/mainserver/global/config/annotation`, `backend/src/main/java/stock/mainserver/global/config/aop`, `backend/src/main/java/stock/mainserver/global/shard`, `backend/src/main/java/stock/mainserver/service/hash` | 실제 구현 없이 구조만 잡혀 있다. | 목적이 불명확한 빈 구조는 이후 모듈 분리 때 더 큰 혼선을 만든다. |

### 5. 서비스 책임이 과도하게 뭉쳐 있음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| `StockService` | `backend/src/main/java/stock/mainserver/service/StockService.java` | Redis 조회, DB fallback, 검색, 카테고리 페이징, DTO 변환, 검색 수 증가, 저장까지 모두 처리한다. | 변경 사유가 너무 많아 단일 책임 원칙을 크게 벗어난다. |
| `AuthService` | `backend/src/main/java/stock/mainserver/auth/service/AuthService.java` | 회원 조회, 비밀번호 검증, 토큰 발급, 쿠키 문자열 생성, 본인 검증까지 함께 처리한다. | 인증 정책과 회원 도메인 정책이 섞여 테스트 경계가 불분명해진다. |
| `HolidayService` | `backend/src/main/java/stock/mainserver/service/init/HolidayService.java` | 초기화, 스케줄링, 외부 API 호출, XML 파싱, 메모리 상태 관리, Redis 발행을 모두 맡는다. | 장애 원인 추적이 어렵고, 외부 연동 실패가 서비스 시작 로직과 직접 얽힌다. |

### 6. Redis 접근 방식이 중복되고 타입 안정성이 약함

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| Redis 키 문자열 하드코딩 | `backend/src/main/java/stock/mainserver/service/StockService.java`, `backend/src/main/java/stock/mainserver/service/IndicesService.java`, `backend/src/main/java/stock/mainserver/service/PopularService.java` | `STOCK:`, `INDICES_INFO:`, `POPULAR` 키가 서비스 내부에 흩어져 있다. | 키 체계가 바뀌면 여러 파일을 동시에 수정해야 하고 누락 위험이 크다. |
| `RedisTemplate<String, Object>` 남용 | `backend/src/main/java/stock/mainserver/service/StockService.java`, `backend/src/main/java/stock/mainserver/service/IndicesService.java`, `backend/src/main/java/stock/mainserver/service/PopularService.java` | 조회 후 `ObjectMapper.convertValue`로 매번 변환한다. | 런타임 타입 오류가 숨어들기 쉽고, 역직렬화 정책이 서비스마다 달라질 수 있다. |
| Redis 관련 추상화 부족 | `backend/src/main/java/stock/mainserver/global/config/RedisConfig.java` | 캐시 접근 전용 저장소 계층 없이 서비스가 직접 접근한다. | 캐시 정책 변경, 로깅, 장애 대응을 한 곳에서 제어하기 어렵다. |

### 7. 도메인 타입과 계산 책임이 뒤섞여 있음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 숫자를 문자열로 보관 | `backend/src/main/java/stock/mainserver/entity/Stock.java`, `backend/src/main/java/stock/mainserver/entity/StockHistory.java` | 가격, 거래량, 증감값이 거의 전부 문자열이다. | 정렬, 비교, 계산 시 매번 파싱해야 해서 버그 가능성이 높다. |
| 정렬을 서비스에서 보정 | `backend/src/main/java/stock/mainserver/service/StockService.java` | 거래대금 정렬을 위해 `volumeValue()`에서 예외를 삼키며 파싱한다. | 데이터 품질 문제를 서비스가 임시 보정하면 잘못된 데이터가 계속 숨어 남는다. |
| 응답 DTO 생성자에서 계산 수행 | `backend/src/main/java/stock/mainserver/dto/response/StockPeriodResponseDto.java` | DTO 생성자가 이전 값 계산까지 담당한다. | 표현 계층 DTO가 비즈니스 계산을 품으면 재사용성과 검증 가능성이 떨어진다. |

### 8. 인증 관련 중복과 계약 불일치가 존재함

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 쿠키 조회 로직 중복 | `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java`, `backend/src/main/java/stock/mainserver/auth/security/JwtAuthenticationFilter.java` | 쿠키를 이름으로 찾는 로직이 두 군데 있다. | 인증 정책 수정 시 한쪽만 고치고 다른 쪽을 놓칠 수 있다. |
| JWT 클레임 네이밍 혼재 | `backend/src/main/java/stock/mainserver/auth/security/JwtTokenProvider.java` | `memberId`, `memberName`, `zipCode`와 함께 `Address`, `AddressDetail`가 섞여 있다. | 직렬화 규칙이 일관되지 않으면 프론트와 토큰 소비 로직이 깨질 수 있다. |
| 컨트롤러가 쿠키 헤더 조립 담당 | `backend/src/main/java/stock/mainserver/auth/controller/AuthController.java` | `withCookies()`로 헤더 조립을 직접 처리한다. | 인증 응답 정책이 컨트롤러마다 중복되기 쉬워진다. |

### 9. 미완성 코드와 정리되지 않은 흔적이 남아 있음

| 대상 | 근거 파일 | 문제 | 왜 리팩토링해야 하는가 |
| --- | --- | --- | --- |
| 빈 서비스 클래스 | `backend/src/main/java/stock/mainserver/service/FxService.java` | 구현 없는 클래스가 존재한다. | 사용 예정 코드인지 죽은 코드인지 구분이 안 되어 탐색 비용을 높인다. |
| 사용하지 않는 DTO | `backend/src/main/java/stock/mainserver/dto/response/CategoriesResponseDto.java` | 현재 사용 흔적이 없다. | DTO가 많아질수록 실제 계약 파악이 더 어려워진다. |
| 테스트 네이밍 불일치 | `backend/src/test/java/stock/mainserver/test/test.java` | 클래스명과 메서드명이 모두 `test`다. | 테스트 목적이 드러나지 않아 실패 원인 추적이 힘들다. |

## 목표 예외 처리 설계

### 설계 원칙

- 비즈니스 예외는 `CoreException` 하나만 사용한다.
- 개별 예외 클래스를 계속 늘리지 않고 `ErrorType`으로 의미를 분류한다.
- `ErrorType`은 최소한 `errorCode`, `message`, `logLevel`, `httpStatus`를 가진다.
- `ExceptionHandler`는 `CoreException`을 받아 `ErrorType` 기준으로 로그와 응답을 만든다.
- 메시지와 코드와 상태값은 예외 클래스가 아니라 `ErrorType`에서 결정한다.

### 권장 구조

```text
core
└── error
    ├── CoreException
    ├── ErrorType
    ├── ErrorResponse
    └── CoreExceptionHandler
```

### 예시 형태

```java
public class CoreException extends RuntimeException {
    private final ErrorType errorType;

    public CoreException(ErrorType errorType) {
        super(errorType.message());
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
```

```java
public interface ErrorType {
    String getErrorCode();
    String getMessage();
    LogLevel getLogLevel();
    HttpStatus getHttpStatus();
}
```

### 권장 운용 방식

- 공통 에러는 `CommonErrorType`
- 인증 관련 에러는 `AuthErrorType`
- 주식 관련 에러는 `StockErrorType`
- 외부 연동 에러는 `ExternalErrorType`

이렇게 도메인별 `ErrorType` 구현 또는 enum으로 나누되, 던지는 예외는 항상 `CoreException`으로 통일하는 방식이 가장 관리하기 쉽다.

### 왜 이 구조가 좋은가

- 예외 클래스 수가 불필요하게 늘어나지 않는다.
- 상태 코드, 메시지, 로그 레벨 변경이 한 군데로 모인다.
- 핸들러가 단순해지고 테스트 포인트가 분명해진다.
- 멀티 모듈 전환 시에도 `core` 모듈이 공통 예외 표준을 제공하기 쉬워진다.

## 목표 패키지 구조 설계

### 상위 패키지 원칙

- `core`: 비즈니스 중심
- `storage`: 저장소와 영속성 중심
- `support`: 인증, 설정, 외부 연동, 공통 유틸 중심

### 권장 패키지 예시

```text
stock.stockzzickmock
├── core
│   ├── api
│   │   ├── stock
│   │   └── auth
│   ├── domain
│   │   ├── stock
│   │   └── member
│   ├── application
│   │   ├── stock
│   │   └── auth
│   ├── error
│   └── response
├── storage
│   ├── db
│   │   ├── stock
│   │   └── member
│   └── redis
│       ├── stock
│       └── market
└── support
    ├── auth
    ├── config
    ├── external
    ├── scheduler
    └── util
```

### 현재 코드 기준 이동 방향

| 현재 위치 | 목표 위치 | 이유 |
| --- | --- | --- |
| `controller` | `core.api` | API 진입점은 비즈니스 모듈의 외부 인터페이스이기 때문이다. |
| `service` | `core.application` | 유스케이스 조합과 흐름 제어는 애플리케이션 계층에 두는 것이 맞다. |
| `entity` | `core.domain` 또는 `storage.db` | 순수 도메인인지 JPA 엔티티인지 먼저 구분해야 한다. 지금 구조는 둘이 섞여 있다. |
| `repository` | `storage.db` | 저장 책임은 storage 쪽으로 밀어야 모듈 경계가 선명해진다. |
| `dto.response`, `dto.request` | `core.api.<domain>.dto` | API 계약 객체는 API와 가까운 곳에 두는 편이 찾기 쉽다. |
| `dto.redis` | `storage.redis` | Redis 저장 형식은 저장 계층 관심사다. |
| `auth/security` | `support.auth` | 인증은 여러 도메인에서 재사용 가능한 지원 기능에 가깝다. |
| `global/config` | `support.config` | 설정은 지원 계층 책임이다. |
| `global/scheduler` | `support.scheduler` | 스케줄링은 비즈니스 핵심보다 운영 지원 기능에 가깝다. |
| `global/error`, `global/handler`, `global/response` | `core.error`, `core.response` | 공통 예외와 공통 응답은 `global`보다 목적 기반 패키지에 두는 편이 명확하다. |

### 왜 이 구조가 멀티 모듈에 유리한가

- `core`는 비즈니스 규칙 중심이라 독립 모듈로 분리하기 쉽다.
- `storage`는 DB, Redis, 구현체를 나누기 좋아 인프라 모듈로 분리하기 쉽다.
- `support`는 인증, 설정, 외부 API처럼 공통 지원 기능을 별도 모듈로 빼기 쉽다.
- 상위 패키지 이름만 봐도 의존 방향을 더 쉽게 통제할 수 있다.

## 권장 리팩토링 순서

### 1단계

- `CoreException`, `ErrorType`, `CoreExceptionHandler`, `ErrorResponse` 표준을 먼저 만든다.
- 기존 개별 예외를 새 구조로 치환한다.
- 컨트롤러와 서비스에서 직접 메시지와 상태값을 결정하는 코드를 제거한다.

### 2단계

- `global` 패키지를 해체하고 `core`, `storage`, `support` 틀을 먼저 만든다.
- 이동 대상이 명확한 공통 클래스부터 옮긴다.
- 특히 `config`, `security`, `handler`, `response`, `repository`, `dto.redis`를 우선 이동한다.

### 3단계

- `StockService`, `AuthService`, `HolidayService`를 `application` 계층 기준으로 분리한다.
- Redis 접근과 DB 접근은 `storage` 구현체 뒤로 숨긴다.
- 도메인 계산 로직과 DTO 매핑 로직을 분리한다.

## 바로 손대기 좋은 1차 리팩토링 순서

1. `CoreException`과 `ErrorType` 기반 예외 표준을 먼저 도입한다.
2. `StockGlobalExceptionHandler`를 대체할 공통 핸들러를 만들고 로그 레벨도 `ErrorType` 기준으로 처리한다.
3. `global` 패키지를 제거하고 `core`, `storage`, `support` 상위 구조를 만든다.
4. 응답 객체를 `ApiResponse` 같은 하나의 규약으로 통합하고 `Success`와 `success`를 하나로 맞춘다.
5. `StockApiController`와 `StockRealTimeController`를 기능 기준으로 다시 나누거나 합쳐서 기준을 명확히 정한다.
6. `StockService`와 `AuthService`를 `application` 계층 기준으로 재분해한다.
7. `String`으로 저장한 가격, 거래량 관련 필드의 타입 전략을 정한다.
8. 미사용 클래스, 미사용 예외, 빈 패키지, 불필요한 요청 필드를 정리한다.

## 메모

- 이번 문서는 구현 변경이 아니라 리팩토링 우선순위 분석 문서다.
- 다음 단계에서는 위 항목 중 `CoreException/ErrorType 표준화`와 `core/storage/support 패키지 재편`부터 시작하는 것이 가장 효과적이다.
