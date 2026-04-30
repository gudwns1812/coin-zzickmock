# Release Docs

이 디렉터리는 `coin-zzickmock`의 배포, 릴리즈, 롤백 운영 문서를 모아 두는 곳이다.
루트의 [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)가 입구와 강한 기준을 담당한다면, 이곳은 환경 정의, 체크리스트, 운영 절차의 원문을 담당한다.

## How To Use

- 먼저 [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md)를 읽는다.
- 그다음 현재 작업에 맞는 상세 문서로 이동한다.
- 실제 배포 자동화나 운영 스크립트를 추가할 때는 이 문서를 먼저 갱신한다.

## Available Release Areas

### Environments And Artifacts

- [01-environments-and-artifacts.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/01-environments-and-artifacts.md)
  환경 분류, 산출물, 설정값, 릴리즈 기록 기준.

### Release Checklist

- [02-release-checklist.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/02-release-checklist.md)
  릴리즈 전/중/후 실행 체크리스트와 중단 조건.

### Rollout And Rollback

- [03-rollout-and-rollback.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/03-rollout-and-rollback.md)
  롤아웃 순서, 호환성 원칙, 롤백과 장애 대응 기준.

### Observability

- [observability/backend-observability-signal-map.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/observability/backend-observability-signal-map.md)
  backend 관측성 metric, trace, log 우선순위와 릴리즈 확인 기준.

### Release Record Template

- [release-note-template.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/release-note-template.md)
  릴리즈 노트와 운영 기록을 남길 때 시작점으로 쓰는 템플릿.
