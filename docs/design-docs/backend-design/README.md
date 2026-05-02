# Backend Design

이 디렉터리는 `coin-zzickmock` 백엔드 상세 설계의 원문을 책임별 문서로 나눠 보관한다.
루트의 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)가 입구와 작업 기준이라면, 이곳은 그 뒤에서 실제 구조 규칙을 설명하는 세부 설계 묶음이다.

## How To Read

백엔드 작업에서는 아래 순서를 기본값으로 사용한다.

1. [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
2. 이 [README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
3. 현재 작업 주제에 맞는 번호 문서

한 번에 모든 번호 문서를 읽기보다, 현재 작업에 필요한 문서를 정확히 골라서 읽는 편이 낫다.
예를 들어 레이어 구조를 바꾸는 작업과 DB 영속성 규칙을 확인하는 작업은 같은 파일을 읽지 않아도 된다.

## Responsibility Split Rule

이 디렉터리의 상세 설계는 "한 문서, 한 1차 책임" 원칙으로 유지한다.

강한 규칙:

- 하나의 문서가 구조, Provider, DB, 네이밍, 테스트, 린트처럼 서로 다른 주제의 원문을 동시에 떠안기 시작하면 분리한다.
- 새 규칙을 추가할 때는 가장 큰 번호 문서 하나에 계속 덧붙이지 않는다.
- 규칙이 둘 이상의 주제에 걸치면 각 문서를 나눠 갱신하고, 이 `README.md`의 문서 설명도 함께 갱신한다.
- 에이전트는 backend 상세 설계를 수정할 때 어느 번호 문서가 원문인지 먼저 판단한 뒤 편집한다.
- 적절한 원문 문서가 없으면 기존 문서를 비대하게 키우기보다 새 번호 문서를 추가한다.

## Documents

- [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
  백엔드 목표 구조, 고정 레이어, 어떤 작업에서 어떤 세부 문서를 읽어야 하는지 정하는 첫 진입 문서.

- [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
  패키지 형태, concrete class 우선 원칙, bean wiring boundary, Spring configuration 배치 규칙과 Spring MVC 요청 경계 예외.

- [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
  dependency rule, `Providers`, application service 경계, 공유 메커니즘 분리, 캐시 경계, 유스케이스 형태.

- [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)
  도메인 모델, 정책, 상태 전이, 값 검증, domain/application 경계 판단의 원문 문서.

- [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
  테스트 레이어 기준과 `architectureLint` 계약, 로그 조회 방법.

- [06-persistence-external-and-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-external-and-exception-rules.md)
  영속성, 외부 연동, 예외 번역, 기술 중심 네이밍 규칙.

- [07-clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/07-clean-code-responsibility.md)
  클래스와 메서드 단위의 책임 분리, 클린 코드 기준, 계획/구현/검증 체크리스트.

## Task Guide

### 구조나 새 기능 패키지를 설계할 때

1. [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
2. [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)
3. [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)

### 유스케이스, Provider, 캐시, 서비스 경계를 수정할 때

1. [01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)
2. [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
3. [07-clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/07-clean-code-responsibility.md)
4. 필요하면 [02-package-and-wiring.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/02-package-and-wiring.md)

### 클래스나 메서드 책임 분리, 클린 코드 기준을 확인할 때

1. [07-clean-code-responsibility.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/07-clean-code-responsibility.md)
2. 유스케이스 경계가 걸리면 [03-application-and-providers.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/03-application-and-providers.md)
3. 도메인 규칙이 걸리면 [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)

### 도메인 모델, 정책, 상태 전이 규칙을 볼 때

1. [04-domain-modeling-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/04-domain-modeling-rules.md)

### DB, repository, QueryDSL, 외부 연동 규칙을 볼 때

1. [06-persistence-external-and-exception-rules.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/06-persistence-external-and-exception-rules.md)
2. [docs/generated/db-schema.md](/Users/hj.park/projects/coin-zzickmock/docs/generated/db-schema.md)

### 테스트 전략이나 아키텍처 린트 규칙을 확인할 때

1. [05-testing-and-lint.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/05-testing-and-lint.md)
2. [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)

## Maintenance Rule

- 번호 문서를 새로 만들면 이 `README.md`에 링크와 짧은 설명을 추가한다.
- 루트 [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)의 읽기 순서가 바뀌면 이 문서도 함께 맞춘다.
- 더 이상 "백엔드 설계 원문 전체"를 설명하는 단일 문서를 다시 만들지 않는다.
