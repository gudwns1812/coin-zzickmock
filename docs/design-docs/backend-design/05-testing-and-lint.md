# Backend Testing And Architecture Lint

## Purpose

이 문서는 백엔드 테스트 레이어 기준과 `architectureLint` 계약을 설명한다.
구조 규칙을 어떤 테스트로 고정하는지, Gradle 검증에서 무엇이 실패해야 하는지, lint 로그를 어떻게 읽어 다음 수정 루프로 연결하는지를 여기서 본다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

## Test Rule

테스트도 레이어 경계를 반영해야 한다.

- domain: 빠른 단위 테스트
- application: 유스케이스 테스트, provider/필요한 계약 fake 사용
- infrastructure: repository/connector 통합 테스트
- api: controller slice 또는 통합 테스트

중요:

- application 테스트는 `Providers` fake로 인증/플래그/텔레메트리 조건을 통제할 수 있어야 한다.
- 테스트 더블이 필요하다는 이유만으로 production 인터페이스를 먼저 만들지 않는다.
- 외부 시스템 통합 테스트는 실제 SDK 호출보다 adapter 계약 검증에 집중한다.
- DB가 필요한 테스트는 기본적으로 인메모리 `H2`를 사용한다.
- 테스트용 DDL/쿼리는 가능하면 MySQL과 의미 차이가 나지 않게 유지하고, 차이가 생기면 그 이유를 테스트 코드나 설정에서 드러낸다.

## Architecture Lint Contract

백엔드에는 테스트 외에 프로젝트 전용 아키텍처 린트를 둔다.

- Gradle task: `./gradlew architectureLint`
- 통합 검증: `./gradlew check`
- 리포트 경로: `backend/build/reports/architecture-lint/violations.jsonl`

이 린터는 일반 스타일 린터가 아니라 구조 린터다.

- 루트 패키지에는 `*Application`만 허용하고, 최상위 하위 패키지는 `common`, `providers`, `feature`만 허용
- `support`, `core`, `extern`, `storage` 같은 레거시 광역 패키지 금지
- `feature.<name>.<layer>` 구조 강제
- `domain` 레이어의 Spring/JPA 의존 금지
- `application` 레이어의 SecurityContext 직접 접근 금지
- `api` 레이어의 persistence import 직접 의존 금지
- `application/service`가 다른 `application/service`를 직접 참조하는 구조 금지

로그 출력 규칙:

- stdout에도 JSON line으로 출력한다.
- 같은 내용을 `violations.jsonl`에도 남긴다.
- 각 레코드는 `tool`, `event`, `rule`, `file`, `line`, `message`, `suggestedFix`를 가진다.

권장 조회 예:

- `rg '"event":"architecture_violation"' backend/build/reports/architecture-lint/violations.jsonl`
- `rg '"rule":"DOMAIN_FRAMEWORK_FREE"' backend/build/reports/architecture-lint/violations.jsonl`

즉, 루프는 아래처럼 동작한다.

1. `./gradlew architectureLint --console=plain`
2. JSONL violation 확인
3. `message`와 `suggestedFix`를 다음 수정 입력으로 사용
4. 수정 후 다시 lint 실행

## Related Documents

- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [06-persistence-external-and-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-external-and-exception-rules.md)
