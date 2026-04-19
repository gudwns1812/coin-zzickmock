# 품질 점수 산정에 multi-angle-review 실제 실행을 강제한다

이 계획서는 [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)와 [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)를 따른다.
이 문서는 품질 게이트 문서가 `multi-angle-review`를 실제로 실행하도록 강제하고, reviewer subagent에 어떤 지시를 줘야 하는지까지 현재 저장소 기준에 맞게 명확히 적는 작업의 단일 기준서다.
사용자가 즉시 실행을 승인했으므로 본 문서는 승인 직후 상태로 `active`에 둔다.

## 목적 / 큰 그림

현재 문서에는 `multi-angle-review` 방식으로 검토한다고 적혀 있지만, 실제 기록에는 수동 5각도 self-review 점수가 남는 경우가 있었다.
이 상태는 "문서상 필수"와 "실제 운영"이 어긋나는 문제를 만든다.

이 작업이 끝나면 품질 점수는 실제 `multi-angle-review` 실행 결과로만 기록해야 하고, reviewer마다 어떤 입력을 주고 무엇을 금지해야 하는지도 문서에 남는다.
그래서 이후 작업자는 점수만 흉내 내는 수동 검토가 아니라, 저장소 기준에 맞는 실제 multi-angle review 실행 절차를 따라야 한다.

## 진행 현황

- [x] (2026-04-17 22:14+09:00) 작업 범위 확인 완료: `QUALITY_SCORE.md`, `CI_WORKFLOW.md`, 작업용 ExecPlan만 수정 대상으로 확정
- [x] (2026-04-17 22:15+09:00) 계획 문서 작성 및 승인 상태 반영 완료
- [x] (2026-04-17 22:20+09:00) `QUALITY_SCORE.md`에 실제 `multi-angle-review` 실행 의무, reviewer 입력 계약, 수동 대체 금지 규칙 반영 완료
- [x] (2026-04-17 22:22+09:00) `CI_WORKFLOW.md`에 Stage 4 통과 조건과 blocker 규칙을 실제 skill 실행 기준으로 정렬 완료
- [x] (2026-04-17 22:23+09:00) 문서 간 교차 검토 및 변경 요약 정리 완료: `rg`, `sed`, fresh `multi-angle-review`로 정합성 재확인
- [ ] PR 생성

## 놀라움과 발견

- 관찰:
  최근 활성/완료 계획 문서에는 `multi-angle-review`를 실제로 실행했다는 기록보다 "수동 5각도 검토"와 final score만 남은 사례가 있었다.
  증거:
  [docs/exec-plans/active/2026-04-17-application-service-dependency-ban.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active/2026-04-17-application-service-dependency-ban.md),
  [docs/exec-plans/completed/2026-04-17-market-cache-strategy-spring-cache-redis.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed/2026-04-17-market-cache-strategy-spring-cache-redis.md).

- 관찰:
  기존 `QUALITY_SCORE.md`는 reviewer prompt와 review output 계약이 느슨해서, review target, 관련 기준 문서, 테스트 결과, 금지 입력을 얼마나 엄격히 넣어야 하는지가 약했다.
  증거:
  [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)의 `Reviewer Prompt Contract`, `Review Inputs`, `Required Review Output` 섹션.

## 의사결정 기록

- 결정:
  품질 점수는 실제 `multi-angle-review` 실행 결과로만 공식 기록으로 인정한다.
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

- `QUALITY_SCORE.md`는 실제 `multi-angle-review` 실행이 없으면 공식 점수 기록과 pass 판정을 하지 못하도록 강화했다.
- reviewer prompt 문구를 "권장"에서 "필수 입력 계약" 수준으로 구체화했다.
- `CI_WORKFLOW.md`는 품질 세부 기준의 소유권을 `QUALITY_SCORE.md`에 두고, Stage 4는 실제 skill 실행 여부와 workflow consequence만 연결하도록 정리했다.
- 문서 전용 작업의 검증 예외도 `QUALITY_SCORE.md` 해석과 충돌하지 않게 맞췄다.
- reviewer 입력/출력과 PR 본문에 민감정보를 복제하지 않도록 마스킹 규칙도 추가했다.
- 남은 일은 PR 단계뿐이다.

## 맥락과 길잡이

관련 문서:

- [QUALITY_SCORE.md](/Users/hj.park/projects/coin-zzickmock/QUALITY_SCORE.md)
- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
- [AGENTS.md](/Users/hj.park/projects/coin-zzickmock/AGENTS.md)
- [/Users/hj.park/.codex/skills/multi-angle-review/SKILL.md](/Users/hj.park/.codex/skills/multi-angle-review/SKILL.md)

이 작업에서 말하는 `multi-angle-review`는 다섯 reviewer subagent를 서로 독립적으로 실행한 뒤 결과를 합성하는 Codex skill 이름이다.
단순히 사람이 readability/performance/security/test/architecture 다섯 관점을 머릿속으로 나눠 보는 것을 뜻하지 않는다.

문서 기준상 정리해야 하는 핵심은 두 가지다.
첫째, 품질 점수의 공식 출처가 실제 skill 실행 결과여야 한다.
둘째, skill을 쓸 때 reviewer마다 어떤 입력을 넣고 어떤 정보는 넣지 말아야 하는지가 문서에 남아 있어야 한다.

## 작업 계획

먼저 `QUALITY_SCORE.md`에서 `Review Panel`, `Reviewer Prompt Contract`, `Review Inputs`, `Required Review Output`, `Default Agent Policy`를 모두 읽고, 수동 self-review도 같은 점수 체계로 인정될 수 있는 여지를 걷어낸다.
그다음 실제 `multi-angle-review` 사용을 전제로, 각 reviewer에 제공해야 하는 입력을 "review target", "관련 기준 문서", "테스트 결과", "TDD 흔적", "제약사항"으로 고정하고, prior review나 예상 답안을 넣지 못하게 금지 규칙을 추가한다.
또한 raw artifact 입력과 review output, PR 본문에 민감값을 그대로 남기지 않도록 마스킹 규칙을 함께 넣는다.

이후 `CI_WORKFLOW.md`의 Stage 4를 같은 기준으로 맞춰, 실제 skill 실행 결과와 `Required Review Output`이 없으면 pass나 final score 기록으로 보지 않도록 정리한다.
마지막으로 문서끼리 용어와 종료 조건이 충돌하지 않는지 다시 확인한다.

## 구체적인 단계

1. `QUALITY_SCORE.md`의 review 관련 섹션을 실제 skill 실행 기준으로 수정한다.
2. reviewer prompt 예시를 저장소 기준에 맞는 필수 입력 계약 형태로 다시 쓴다.
3. `CI_WORKFLOW.md` Stage 4에 수동 대체 점수 금지와 blocker 규칙을 추가한다.
4. `rg`로 `multi-angle-review`, `수동 5각도`, `final score` 관련 표현을 다시 찾아 상충이 없는지 확인한다.
5. 변경 범위를 짧게 self-check 하고 결과를 이 계획서에 남긴다.

## 검증과 수용 기준

실행 명령:

- `rg -n "multi-angle-review|수동 5각도|Required Review Output|문서 전용 작업|민감값|마스킹|final score" QUALITY_SCORE.md CI_WORKFLOW.md`
- `sed -n '1,220p' QUALITY_SCORE.md`
- `sed -n '260,430p' QUALITY_SCORE.md`
- `sed -n '210,290p' CI_WORKFLOW.md`
- 실제 `multi-angle-review`로 이 세 파일만 범위 지정해 fresh reviewer 5개를 다시 실행한다.

수용 기준:

- `QUALITY_SCORE.md`는 공식 final score가 실제 `multi-angle-review` 실행 결과여야 한다고 명시한다.
- `QUALITY_SCORE.md`는 reviewer마다 반드시 넣을 입력과 넣지 말아야 할 편향 정보를 명시한다.
- `QUALITY_SCORE.md`와 `CI_WORKFLOW.md`는 reviewer 입력, review output, PR 본문에 민감값 원문을 남기지 않도록 명시한다.
- `CI_WORKFLOW.md`는 실제 skill 실행 결과가 없으면 Stage 4 통과로 보지 않는다고 명시한다.
- 문서 어디에도 수동 5각도 검토를 공식 점수 대체 수단처럼 읽히는 문구가 남지 않는다.

## 반복 실행 가능성 및 복구

- 이 작업은 문서 수정만 포함하므로 반복 실행해도 환경을 깨뜨리지 않는다.
- 이미 다른 코드 변경이 워킹트리에 있으므로, commit 단계에서는 이번 문서 파일만 선택적으로 다뤄야 한다.
- 문구가 과도하게 강해져 실제 운영을 막는다고 판단되면, 후속 수정도 같은 문서 범위 안에서 안전하게 되돌릴 수 있다.

## 산출물과 메모

- 예상 PR 제목:
  품질 점수 산정에 multi-angle-review 실제 실행을 강제한다
- 변경 메모:
  이 작업은 구현 코드가 아니라 운영 기준 문서를 정렬하는 작업이다. 따라서 테스트 대신 문서 간 상충 여부, 민감정보 마스킹 규칙, 실제 `multi-angle-review` 재검토 결과를 검증한다.
