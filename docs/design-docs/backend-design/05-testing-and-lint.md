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
- web: controller/SSE delivery slice 또는 통합 테스트
- job: trigger가 application service/coordinator만 호출하는지 확인하는 얇은 단위 테스트

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
- 최종 feature layer는 `web`, `job`, `application`, `domain`, `infrastructure`로 고정
- `api` Java package는 금지하고 HTTP/SSE delivery는 `web` package 아래에 둠
- `domain` 레이어의 Spring/JPA 의존 금지
- `application` 레이어의 SecurityContext 직접 접근 금지
- `web` 레이어의 persistence/infrastructure import 직접 의존 금지
- feature scheduler/startup/background trigger는 `job` package 아래에 둠
- `job` 레이어의 persistence/entity/provider infrastructure/feature infrastructure/HTTP/SSE 직접 의존 금지
- `application/service`가 다른 `application/service`를 직접 참조하는 구조 금지

### Lint Ratchet

`web/job` migration은 한 번에 모든 rule을 fail로 켜지 않는다.
`architectureLint`는 아래 순서로 강화한다.

1. Inventory report: old `api`, misplaced trigger, web-owned class, config split 후보를 `architecture_advisory`로 출력하고 fail하지 않는다.
2. Advisory warning: 새 규칙 위반을 계속 report하되 migration 중 CI를 막지 않는다.
3. Slice-specific fail: 해당 PR이 소유한 위반만 fail로 전환한다.
4. Final strict: `api` layer와 inbound-infrastructure drift를 금지한다.

PR-1에서 시작한 advisory rule:

- `WEB_OWNS_HTTP_DELIVERY_ADVISORY`
- `INFRASTRUCTURE_CONFIG_SPLIT_ADVISORY`

`FEATURE_API_LAYER_MIGRATION_TARGET` advisory는 PR-2 package migration 이후 `FEATURE_LAYER_REQUIRED` 실패 조건으로 흡수되었다.
`JOB_OWNS_BACKGROUND_TRIGGERS_ADVISORY` advisory는 PR-3 trigger extraction 이후 `JOB_OWNS_BACKGROUND_TRIGGERS` 실패 조건으로 흡수되었다.
이 advisory는 `violations`에 포함되지 않으며 `architectureLint` 성공 여부를 바꾸지 않는다.
후속 PR에서 같은 개념의 strict rule을 켤 때는 해당 PR이 현재 위반도 함께 제거해야 한다.

로그 출력 규칙:

- stdout에도 JSON line으로 출력한다.
- 같은 내용을 `violations.jsonl`에도 남긴다.
- 각 violation/advisory 레코드는 `tool`, `event`, `rule`, `file`, `line`, `message`, `suggestedFix`를 가진다.

권장 조회 예:

- `rg '"event":"architecture_violation"' backend/build/reports/architecture-lint/violations.jsonl`
- `rg '"event":"architecture_advisory"' backend/build/reports/architecture-lint/violations.jsonl`
- `rg '"rule":"DOMAIN_FRAMEWORK_FREE"' backend/build/reports/architecture-lint/violations.jsonl`

즉, 루프는 아래처럼 동작한다.

1. `./gradlew architectureLint --console=plain`
2. JSONL violation 확인
3. `message`와 `suggestedFix`를 다음 수정 입력으로 사용
4. 수정 후 다시 lint 실행

## Related Documents

- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [.github/workflows/ci.yml](/Users/hj.park/projects/coin-zzickmock/.github/workflows/ci.yml)
- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
- [06-persistence-external-and-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-external-and-exception-rules.md)
