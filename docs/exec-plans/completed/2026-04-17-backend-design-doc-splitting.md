# backend 상세 설계 문서 책임 분리와 강제 규칙 정리

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 `docs/design-docs/backend-design` 아래의 과적재된 상세 설계 문서를 책임별 파일로 분리하고, 이후 에이전트가 다시 하나의 거대한 파일에 규칙을 누적하지 못하도록 루트 입구 문서와 인덱스 문서에 강제 규칙을 추가하는 작업의 단일 기준서다.
이번 요청은 사용자가 직접 "확인하고 분리하고 강제하자"라고 지시한 문서 구조 정리 작업이므로, 본 문서는 사용자 요청을 승인 신호로 해석한 상태에서 `active`에 둔다.

## 목적 / 큰 그림

현재 `docs/design-docs/backend-design/01-architecture-foundations.md` 한 파일에 레이어, 의존 규칙, Provider, 빈 조립, DB, 네이밍, 테스트, 린트 규칙이 모두 들어 있다.
이 구조는 사람이 읽기에도 길고, 특히 Codex 같은 에이전트가 "지금 필요한 섹션만 안정적으로 다시 읽는" 데 실패하기 쉽다.

이 작업이 끝나면 backend 상세 설계는 책임별 문서 묶음으로 나뉘고, `README.md`, `BACKEND.md`, `DESIGN.md`, `AGENTS.md`, `CI_WORKFLOW.md`가 모두 같은 읽기 순서를 가리키게 된다.
또한 에이전트가 backend 규칙을 추가하거나 수정할 때는 "적절한 세부 문서에 넣고 인덱스도 같이 갱신한다"는 강한 규칙이 문서로 고정되어, 다시 단일 비대 문서로 회귀하기 어려워져야 한다.

## 진행 현황

- [x] (2026-04-17 21:53+09:00) 현재 backend 상세 설계 구조 조사 완료: `backend-design`에는 `README.md`와 `01-architecture-foundations.md` 한 파일만 있고, 핵심 규칙이 528줄짜리 단일 문서에 집중되어 있음을 확인
- [x] (2026-04-17 21:54+09:00) 분리 대상 책임 묶음 초안 확정: 구조/레이어, 패키지/조립, Provider/application 경계, 영속성/예외/네이밍, 테스트/린트로 나누는 방향 결정
- [x] (2026-04-17 21:58+09:00) backend 상세 설계 문서를 `README + 01~05` 책임별 파일로 분리하고 새 인덱스를 작성
- [x] (2026-04-17 21:59+09:00) 루트 입구 문서와 에이전트 인덱스에 새 읽기 순서와 분리 강제 규칙 반영
- [x] (2026-04-17 22:00+09:00) 링크와 참조 흐름 점검 완료: `rg` 검색으로 루트/인덱스 문서가 새 번호 문서를 가리키는지 확인

## 놀라움과 발견

- 관찰:
  현재 루트 `BACKEND.md`도 상세 설계 원문을 사실상 `01-architecture-foundations.md` 한 파일에 의존하고 있다.
  증거:
  [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)의 상세 설계 링크와 읽기 순서가 모두 `01-architecture-foundations.md`를 단일 진입점으로 가리킨다.

- 관찰:
  저장소의 `DESIGN.md`는 이미 "길어지면 01-, 02- 식으로 쪼개라"고 말하지만, backend 상세 설계에는 그 규칙이 실제로 적용되지 못했다.
  증거:
  [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)의 `Recommended Shape For Design Docs` 섹션과, 실제 [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)의 단일 문서 구조가 서로 어긋난다.

- 관찰:
  `01-architecture-foundations.md`를 구조/읽기 순서 문서로 축소한 뒤에도 세부 규칙 원문은 4개 후속 문서로 충분히 분산됐다.
  증거:
  `wc -l docs/design-docs/backend-design/*.md` 결과에서 기존 528줄짜리 단일 문서 대신 `01` 156줄, `02` 124줄, `03` 172줄, `04` 136줄, `05` 72줄로 나뉜 것이 확인됐다.

## 의사결정 기록

- 결정:
  기존 `01-architecture-foundations.md`는 삭제하지 않고, 가장 먼저 읽어야 하는 "구조/레이어/읽기 순서" 문서로 축소한 뒤 나머지 책임은 후속 번호 문서로 분리한다.
  근거:
  이미 여러 루트 문서가 `01-architecture-foundations.md`를 링크하고 있으므로 파일 자체를 없애기보다 역할을 가볍게 재정의하는 편이 링크 안정성과 이해 비용 면에서 낫다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  강제 규칙은 backend 상세 설계 폴더 안에만 두지 않고 `BACKEND.md`, `DESIGN.md`, `CI_WORKFLOW.md`, `AGENTS.md`까지 끌어올린다.
  근거:
  에이전트가 처음 읽는 문서는 루트 입구 문서와 인덱스 문서이기 때문에, 분리 규칙이 상세 폴더 안에만 있으면 다시 놓칠 가능성이 높다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- `docs/design-docs/backend-design`은 이제 `README.md`와 `01`부터 `05`까지의 책임별 번호 문서 구조가 됐다.
- 기존 `01-architecture-foundations.md`는 더 이상 모든 규칙 원문을 떠안지 않고, 구조 목표와 읽기 순서를 안내하는 첫 진입 문서로 축소됐다.
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md), [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md), [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md), [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md), [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)가 모두 새 인덱스와 책임 분리 규칙을 가리키도록 갱신됐다.
- 이번 작업의 목표였던 "backend 상세 설계의 파일 단위 책임 분리"와 "다음 에이전트가 단일 비대 문서로 회귀하기 어렵게 만드는 입구 규칙 추가"를 둘 다 달성했다.

## 맥락과 길잡이

관련 문서:

- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
- [DESIGN.md](/Users/hj.park/projects/coin-zzickmock/DESIGN.md)
- [BACKEND.md](/Users/hj.park/projects/coin-zzickmock/BACKEND.md)
- [docs/design-docs/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/README.md)
- [docs/design-docs/backend-design/README.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/README.md)
- [docs/design-docs/backend-design/01-architecture-foundations.md](/Users/hj.park/projects/coin-zzickmock/docs/design-docs/backend-design/01-architecture-foundations.md)

이번 작업에서 말하는 "책임 분리"는 단순히 파일 개수를 늘리는 것이 아니다.
한 문서가 여러 독립 주제의 원문까지 한꺼번에 떠안지 않도록, 읽기 목적이 다른 규칙을 별도 파일로 나누는 것을 뜻한다.
예를 들어 레이어 구조를 찾는 사람과 DB/QueryDSL 규칙을 찾는 사람은 서로 다른 문서에서 바로 답을 찾아야 한다.

## 작업 계획

먼저 `backend-design/01-architecture-foundations.md`의 현재 섹션을 기준으로 어떤 책임 묶음이 있는지 정리하고, 새 문서 경계를 확정한다.
기본 방향은 아래와 같다.

- `01-architecture-foundations.md`: 백엔드 목표 구조, 고정 레이어, 읽기 순서, 어떤 문서를 언제 읽어야 하는지
- `02-package-and-wiring.md`: 패키지 형태, concrete class 우선, bean wiring boundary, Spring configuration 경계
- `03-application-and-providers.md`: dependency rule, provider 구조, application service/use case/caching 경계
- `04-persistence-and-domain-rules.md`: domain naming, persistence/external system, exception, naming 규칙
- `05-testing-and-lint.md`: 테스트 경계와 architecture lint 계약

그 다음 `backend-design/README.md`를 새 목차로 다시 쓰고, 루트 `BACKEND.md`와 `AGENTS.md`의 백엔드 진입 순서를 `README -> 필요한 세부 문서` 구조로 바꾼다.
마지막으로 `DESIGN.md`와 `CI_WORKFLOW.md`에는 "상세 설계 규칙을 큰 문서 하나에 덧붙이지 말고 책임별 문서와 인덱스를 함께 갱신한다"는 강한 문장으로 문서 구조 회귀를 막는다.

## 구체적인 단계

1. 기존 backend 상세 설계 문서의 섹션과 규칙을 책임별로 재배치한다.
2. 새 번호 문서와 `README.md`를 작성한다.
3. `BACKEND.md`, `DESIGN.md`, `docs/design-docs/README.md`, `AGENTS.md`, `CI_WORKFLOW.md`를 새 구조에 맞게 갱신한다.
4. 링크 검색으로 깨진 참조가 없는지 확인한다.
5. 이 계획서의 `진행 현황`, `의사결정 기록`, `결과 및 회고`를 실제 결과로 갱신한다.

## 검증과 수용 기준

수용 기준:

- `docs/design-docs/backend-design` 아래에 하나의 비대 문서 대신 책임별 번호 문서와 인덱스가 존재한다.
- `BACKEND.md`는 더 이상 단일 상세 문서만 읽으라고 하지 않고, `README.md`와 주제별 문서로 안내한다.
- `DESIGN.md`, `CI_WORKFLOW.md`, `AGENTS.md` 중 적어도 에이전트가 초기에 읽는 문서들에 "상세 설계는 책임별 파일로 유지하고 인덱스를 같이 갱신한다"는 규칙이 반영된다.
- 저장소 내부 링크가 새 파일 구조를 가리키고, 제거된 링크가 남아 있지 않다.

## 반복 실행 가능성 및 복구

- 이번 작업은 문서 구조 변경만 포함하므로 DB나 런타임 복구 절차는 필요 없다.
- 문서 분리 중간에 멈춰도 `README.md`와 루트 입구 문서가 마지막 상태를 가리키기 전까지는 완료로 보면 안 된다.
- 번호 문서를 추가한 뒤 기존 문서를 축소하는 순서로 진행하면, 중간 상태에서도 링크가 완전히 사라지는 시간을 줄일 수 있다.

## 산출물과 메모

- 예상 산출물:
  `docs/design-docs/backend-design/02-*.md` 이상 새 문서들, 갱신된 `README.md`, 루트 입구 문서의 강제 규칙
- 변경 메모:
  초안에서는 backend 상세 설계의 과적재 상태와 루트 문서의 단일 파일 의존을 먼저 고정했다. 실제 작업에서는 `README + 01~05` 구조로 분리하고, 루트 입구 문서와 워크플로우 문서까지 함께 갱신했다.
