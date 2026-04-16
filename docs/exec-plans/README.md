# Exec Plans

이 디렉터리는 실행 계획 문서를 관리하는 곳이다.

## Structure

- [active](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/active)
  승인되어 현재 진행 중인 계획
- [completed](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/completed)
  merge 완료 또는 stale/superseded 종료 후 보관하는 계획
- [plan-template.md](/Users/hj.park/projects/coin-zzickmock/docs/exec-plans/plan-template.md)
  새 계획을 만들 때 시작점으로 쓰는 템플릿

## Workflow

1. Plan mode로 계획 초안을 만든다.
2. 사용자 승인을 받는다.
3. 승인되면 `active/` 아래에 둔다.
4. 구현과 함께 문서를 계속 갱신한다.
5. merge가 끝나거나 계획이 stale/superseded로 닫히면 `completed/`로 이동한다.

상세 규칙은 아래 문서를 따른다.

- [PLANS.md](/Users/hj.park/projects/coin-zzickmock/PLANS.md)
- [CI_WORKFLOW.md](/Users/hj.park/projects/coin-zzickmock/CI_WORKFLOW.md)
