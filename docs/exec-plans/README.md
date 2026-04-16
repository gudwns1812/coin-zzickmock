# Exec Plans

이 디렉터리는 실행 계획 문서를 관리하는 곳이다.

## Structure

- [active](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active)
  승인되어 현재 진행 중인 계획
- [completed](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed)
  현재 작업이 닫힌 뒤 보관하는 계획
- [plan-template.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/plan-template.md)
  새 계획을 만들 때 시작점으로 쓰는 템플릿

## Workflow

1. Plan mode로 계획 초안을 만든다.
2. 사용자 승인을 받는다.
3. 승인되면 `active/` 아래에 둔다.
4. 구현과 함께 문서를 계속 갱신한다.
5. 현재 작업이 닫히면 `completed/`로 이동한다.

여기서 "현재 작업"은 Codex 세션 전체가 아니라 개별 계획 문서를 뜻한다.
즉, 해당 계획의 `진행 현황`에 남은 항목이 없으면 `completed/`로 이동하고, 미완료 항목이 남아 있으면 계속 `active/`에 둔다.

상세 규칙은 아래 문서를 따른다.

- [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
