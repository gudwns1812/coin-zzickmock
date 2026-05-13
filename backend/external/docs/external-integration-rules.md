# Backend External Integration Rules

## Purpose

이 문서는 외부 API, SDK, connector 같은 outbound integration 규칙만 소유한다.
DB 영속성 원문은 [06-persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)가,
예외 모델 원문은 [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)가 소유한다.

먼저 읽어야 하는 문서:

- [README.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/README.md)
- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/architecture/foundations.md)
- Provider 경계가 걸리면 [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)

## External System Boundary

`external`은 provider leaf adapter module이다. backend project module 중 `core`에만 의존하며 Bitget connection, reconnect, raw DTO parsing, client/runtime classes, and provider-specific failure translation을 소유한다.
Provider-neutral market contracts/events belong in `core`; Bitget raw WebSocket event classes and DTOs must not leak into `app` web/job or core application/domain code.

외부 연동 구현은 `infrastructure`에 둔다.

권장 구조:

- 외부 연동: `feature/<feature>/infrastructure/connector`
- 외부 응답 매핑: `feature/<feature>/infrastructure/mapper`
- feature와 무관한 플랫폼 연동 조립: `providers/infrastructure/config`

규칙:

- 외부 SDK, HTTP client, provider-specific DTO는 application/domain에 노출하지 않는다.
- application은 외부 시스템의 기술 응답이 아니라 feature 언어로 번역된 결과에 의존한다.
- connector 위에 이유 없는 중간 인터페이스를 하나 더 만들지 않는다.
- 외부 경계를 격리하는 계약이 실제로 필요하면 application/domain 언어의 역할 이름을 사용한다.
- 외부 실패는 infrastructure 또는 application 경계에서 [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)에 맞춰 번역한다.

Bitget 또는 시장 데이터 연동을 수정할 때는 [docs/references/README.md](/Users/hj.park/projects/coin-zzickmock/docs/references/README.md)와 관련 reference 문서를 함께 본다.

## Related Documents

- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/backend/core/docs/application-and-providers.md)
- [06-persistence-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/storage/docs/persistence-rules.md)
- [09-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/errors/exception-rules.md)
- [10-technical-naming-rules.md](/Users/hj.park/projects/coin-zzickmock/backend/docs/code-quality/technical-naming-rules.md)
- [docs/references/README.md](/Users/hj.park/projects/coin-zzickmock/docs/references/README.md)
