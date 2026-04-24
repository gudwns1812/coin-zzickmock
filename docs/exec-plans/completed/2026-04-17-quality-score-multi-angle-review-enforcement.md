# review 스킬 기반 품질 게이트 정렬

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 과거 품질 점수 문서를 review 스킬 중심 운영으로 정렬했던 변경 기록을 보존하는 완료 계획서다.
사용자가 즉시 실행을 승인했으므로 본 문서는 승인 직후 상태로 `active`에 둔다.

## 목적 / 큰 그림

당시에는 review를 점수 문서와 `multi-angle-review` 중심으로 강제하려는 목적이 있었다.
현재 기준에서는 `QUALITY_SCORE.md`를 제거했고, review 게이트는 `AGENTS.md`에 등록된 review 관련 스킬 계약을 따른다.

## 진행 현황

- [x] (2026-04-17 22:14+09:00) 작업 범위 확인 완료: 당시 품질 게이트 문서와 workflow 문서를 정렬 대상으로 확정
- [x] (2026-04-17 22:15+09:00) 계획 문서 작성 및 승인 상태 반영 완료
- [x] (2026-04-17 22:20+09:00) 당시 품질 게이트 문서에 review 실행 의무, reviewer 입력 계약, 수동 대체 금지 규칙 반영 완료
- [x] (2026-04-17 22:22+09:00) `CI_WORKFLOW.md`에 Stage 4 통과 조건과 blocker 규칙을 실제 skill 실행 기준으로 정렬 완료
- [x] (2026-04-17 22:23+09:00) 문서 간 교차 검토 및 변경 요약 정리 완료
- [ ] PR 생성

## 놀라움과 발견

- 관찰:
  최근 활성/완료 계획 문서에는 review 실행 기록보다 수동 점검과 점수 표현이 먼저 남는 사례가 있었다.
  증거:
  [docs/exec-plans/active/2026-04-17-application-service-dependency-ban.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active/2026-04-17-application-service-dependency-ban.md),
  [docs/exec-plans/completed/2026-04-17-market-cache-strategy-spring-cache-redis.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed/2026-04-17-market-cache-strategy-spring-cache-redis.md).

- 관찰:
  기존 review 기준 문서는 reviewer prompt와 review output 계약이 느슨해서, review target, 관련 기준 문서, 테스트 결과, 금지 입력을 얼마나 엄격히 넣어야 하는지가 약했다.
  증거:
  당시 품질 게이트 문서의 `Reviewer Prompt Contract`, `Review Inputs`, `Required Review Output` 섹션.

## 의사결정 기록

- 결정:
  품질 게이트는 실제 review 실행 결과로만 공식 기록으로 인정한다.
  근거:
  저장소 문서가 스킬 사용을 기준으로 삼고 있는데 수동 self-review 점수까지 같은 급으로 허용하면 종료 조건이 흔들린다.
  날짜/작성자:
  2026-04-17 / Codex

- 결정:
  reviewer subagent 프롬프트는 "짧게"만 두지 않고, 반드시 포함할 입력과 절대 넣지 말아야 할 편향 정보를 함께 문서화한다.
  근거:
  실제 스킬 사용 시 결과 품질은 프롬프트 계약에 크게 좌우되므로, 저장소 문서가 최소 입력 계약을 직접 적어 두는 편이 재현 가능성이 높다.
  날짜/작성자:
  2026-04-17 / Codex

## 결과 및 회고

- 당시 품질 게이트 문서는 실제 review 실행이 없으면 공식 pass 판정을 하지 못하도록 강화했다.
- reviewer prompt 문구를 "권장"에서 "필수 입력 계약" 수준으로 구체화했다.
- `CI_WORKFLOW.md`는 Stage 4를 실제 review 실행 여부와 workflow consequence 중심으로 정리했다.
- 문서 전용 작업의 검증 예외도 당시 품질 게이트 해석과 충돌하지 않게 맞췄다.
- reviewer 입력/출력과 PR 본문에 민감정보를 복제하지 않도록 마스킹 규칙도 추가했다.
- 남은 일은 PR 단계뿐이다.

## 맥락과 길잡이

관련 문서:

- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)

현재 기준:

- `QUALITY_SCORE.md`는 제거됐다.
- review 게이트는 [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)의 review 관련 스킬(`code-review`, `security-review` 등) 계약을 따른다.
- 이 문서는 과거 정렬 작업의 기록으로만 남긴다.

문서 기준상 지금 남기는 핵심은 두 가지다.
첫째, review 게이트는 실제 review 스킬 실행 결과를 기준으로 삼아야 한다.
둘째, reviewer 입력과 범위는 문서로 남아 있어야 한다.

## 작업 계획

현재는 이 계획서를 실행 지침으로 쓰지 않는다.
후속 작업자는 `AGENTS.md`와 관련 워크플로우 문서만 읽으면 된다.

## 구체적인 단계

1. 당시 품질 게이트 문서를 review 실행 기준으로 수정했다.
2. reviewer 입력 계약을 문서화했다.
3. workflow 문서와 용어를 정렬했다.
4. 현재는 `QUALITY_SCORE.md` 제거 이후 기록 문서로만 유지한다.

## 검증과 수용 기준

실행 명령:

- `rg -n "QUALITY_SCORE\\.md|품질 점수|final score" .`
- `rg -n "code-review|security-review|review 관련 스킬" AGENTS.md BACKEND.md docs`

수용 기준:

- 현재 기준 문서 어디에도 `QUALITY_SCORE.md`를 필수 기준으로 남기지 않는다.
- review 게이트는 `AGENTS.md` 기반 review 스킬 계약으로 해석된다.

## 반복 실행 가능성 및 복구

- 이 작업은 문서 수정만 포함하므로 반복 실행해도 환경을 깨뜨리지 않는다.
- 이미 다른 코드 변경이 워킹트리에 있으므로, commit 단계에서는 이번 문서 파일만 선택적으로 다뤄야 한다.
- 문구가 과도하게 강해져 실제 운영을 막는다고 판단되면, 후속 수정도 같은 문서 범위 안에서 안전하게 되돌릴 수 있다.

## 산출물과 메모

- 예상 PR 제목:
  review 스킬 기반 품질 게이트 정렬
- 변경 메모:
  이 작업은 구현 코드가 아니라 운영 기준 문서를 정렬하는 작업이다. 현재는 `QUALITY_SCORE.md`가 제거됐으므로, 이 문서는 과거 변경 기록으로만 유지한다.
