# Release Docs

이 디렉터리는 `coin-zzickmock`의 배포, 릴리즈, 롤백 운영 문서를 모아 두는 곳이다.
루트의 [RELEASE.md](../../RELEASE.md)가 입구와 강한 기준을 담당한다면, 이곳은 환경 정의, 체크리스트, 운영 절차의 원문을 담당한다.

## How To Use

- 먼저 [RELEASE.md](../../RELEASE.md)를 읽는다.
- 그다음 현재 작업에 맞는 상세 문서로 이동한다.
- 실제 배포 자동화나 운영 스크립트를 추가할 때는 이 문서를 먼저 갱신한다.

## Available Release Areas

### Environments And Artifacts

- [01-environments-and-artifacts.md](01-environments-and-artifacts.md)
  환경 분류, 산출물, 설정값, 릴리즈 기록 기준.

### Production CD

- [04-production-cd.md](04-production-cd.md)
  Docker Hub backend 이미지 발행, EC2 SSH 배포, 서버 환경 변수 계약.

### Frontend Vercel Operations

- [05-frontend-vercel-operations.md](05-frontend-vercel-operations.md)
  Vercel Git Integration 기반 frontend 배포, 환경 변수, Preview/Production 운영, rollback 기준.

### Release Checklist

- [02-release-checklist.md](02-release-checklist.md)
  릴리즈 전/중/후 실행 체크리스트와 중단 조건.

### Branch And PR Rules

- [../process/branch-and-pr-rules.md](../process/branch-and-pr-rules.md)
  PR과 릴리즈 후보 브랜치가 지켜야 하는 type-prefixed branch naming 규칙.

### Rollout And Rollback

- [03-rollout-and-rollback.md](03-rollout-and-rollback.md)
  롤아웃 순서, 호환성 원칙, 롤백과 장애 대응 기준.

### Observability

- [observability/backend-observability-signal-map.md](observability/backend-observability-signal-map.md)
  backend 관측성 metric, trace, log 우선순위와 릴리즈 확인 기준.
- [observability/local-infra-stack.md](observability/local-infra-stack.md)
  로컬 Docker Compose 기반 Nginx, Prometheus, Grafana, Loki 실행 기준.
- [observability/dau-dashboard.md](observability/dau-dashboard.md)
  DB 기반 DAU 집계와 Grafana 패널 구성을 위한 SQL/PromQL 기준.

### Release Record Template

- [release-note-template.md](release-note-template.md)
  릴리즈 노트와 운영 기록을 남길 때 시작점으로 쓰는 템플릿.
