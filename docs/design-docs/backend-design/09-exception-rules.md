# Backend Exception Rules

## Purpose

이 문서는 백엔드 예외 모델, 예외 번역, HTTP error response 경계만 소유한다.
영속성 원문은 [06-persistence-rules.md](./06-persistence-rules.md)가,
외부 연동 원문은 [08-external-integration-rules.md](./08-external-integration-rules.md)가 소유한다.

먼저 읽어야 하는 문서:

- [README.md](./README.md)
- [01-architecture-foundations.md](./01-architecture-foundations.md)

## Exception Model

예외 모델은 `CoreException` 중심으로 통일한다.

권장 구조:

```text
common/error/
  ErrorCode.java
  CoreException.java
  ErrorResponse.java
  GlobalExceptionHandler.java
```

규칙:

- 도메인/애플리케이션 실패는 구조화된 에러 코드로 표현한다.
- `CoreException`은 기본 예외 타입 하나로 유지하고, `BadRequestException`, `NotFoundException`처럼 상태별 하위 클래스를 기계적으로 만들지 않는다.
- `CoreException`은 `ErrorCode`만 받는다. public/custom/detail message를 받는 생성자를 두지 않는다.
- `ErrorCode`는 HTTP status, 안전한 public message, `org.slf4j.event.Level` 기반 default log level만 소유한다.
- HTTP 응답의 `message`는 항상 `ErrorCode.message()`에서 나온다. `exception.getMessage()`, provider message, DB detail, id/date 같은 진단 문자열을 응답으로 내보내지 않는다.
- `GlobalExceptionHandler`는 모든 `CoreException`을 중앙에서 `ErrorCode` 정책에 따라 로깅하고 `ErrorResponse(code, message)`로 변환한다.
- 중앙 핸들러가 알 수 없는 provider/cache/retry/SSE/notification 같은 boundary-specific context는 예외를 던지기 직전의 feature/provider/application boundary에서 sanitized metadata로만 보완한다.
- 외부 연동 실패는 infrastructure 또는 application 경계에서 번역한다.
- HTTP 응답 변환은 글로벌 핸들러 한 곳에서 수행한다.
- `catch (Exception)`은 경계에서 번역할 때만 허용한다.

### ErrorCode 승격 기준

custom message를 제거할 때는 아래 기준으로 새 `ErrorCode` 승격 여부를 결정한다.

승격한다:

- 사용자가 취할 조치가 기존 코드와 다르다.
- 도메인 상태 전이, 권한, 충돌, 리소스 부재처럼 의미가 안정적인 실패다.
- HTTP status 또는 default log level이 기존 코드와 다르다.
- 프론트/UI, 제품 명세, 외부 클라이언트, 테스트가 구분된 public copy에 의존한다.
- 여러 기능에서 재사용될 수 있는 public contract다.

승격하지 않는다:

- memberId, orderId, requestId, 날짜, cache key, provider message, SQL detail처럼 runtime detail이 섞인다.
- field별 validation 문구처럼 같은 조치로 수렴하는 입력 오류다.
- 기존 `INVALID_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `*_NOT_FOUND`, `*_CONFLICT`와 의미가 중복된다.
- 디버깅 편의를 위한 내부 진단 문자열이다.
- enum 값 하나가 parser branch 하나와 1:1로만 대응하고 제품 copy로 고정되지 않았다.

## Related Documents

- [04-domain-modeling-rules.md](./04-domain-modeling-rules.md)
- [08-external-integration-rules.md](./08-external-integration-rules.md)
- [05-testing-and-lint.md](./05-testing-and-lint.md)
