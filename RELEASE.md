# RELEASE.md

## Purpose

이 문서는 `coin-zzickmock` 저장소의 배포와 릴리즈 운영 기준을 정의하는 입구 문서다.
현재 저장소에는 CI와 Docker Hub/EC2 SSH 기반 backend CD 워크플로가 있다. 이 문서는 "무엇을 먼저 확인해야 하는지", "무엇을 절대 생략하면 안 되는지", "상세 원문이 어디에 있는지"를 짧고 강하게 고정한다.

## What This File Does

이 문서는 아래 역할만 맡는다.

- 배포와 릴리즈 전에 무엇을 읽어야 하는지 알려 준다.
- 환경, 산출물, 릴리즈 기록, 롤백에 대한 최소 기준을 고정한다.
- 상세 운영 문서를 `docs/release-docs/`로 연결한다.

즉, "배포/릴리즈 작업용 체크인 문서"라고 보면 된다.

## Read Order

### 새 배포 환경이나 자동화 파이프라인을 설계할 때

1. [ARCHITECTURE.md](ARCHITECTURE.md)
2. [docs/release-docs/01-environments-and-artifacts.md](docs/release-docs/01-environments-and-artifacts.md)
3. [.github/workflows/ci.yml](.github/workflows/ci.yml)

### 정기 릴리즈나 수동 배포를 실행할 때

1. [docs/release-docs/02-release-checklist.md](docs/release-docs/02-release-checklist.md)
2. [docs/release-docs/03-rollout-and-rollback.md](docs/release-docs/03-rollout-and-rollback.md)
3. [.github/workflows/ci.yml](.github/workflows/ci.yml)

### 장애 대응이나 롤백 판단이 필요할 때

1. [docs/release-docs/03-rollout-and-rollback.md](docs/release-docs/03-rollout-and-rollback.md)
2. [docs/release-docs/01-environments-and-artifacts.md](docs/release-docs/01-environments-and-artifacts.md)
3. 관련 워크스페이스 기준 문서

## Current Reality

현재 저장소의 배포/릴리즈 현실은 아래와 같다.

- `.github/workflows/ci.yml`는 프론트엔드 빌드와 백엔드 `check`를 검증한다.
- `.github/workflows/cd.yml`는 `main`/`master`의 `backend/**`, `docker-compose.prod.yml`, `infra/**` 변경 또는 수동 실행 기준으로 `backend_image`, `backend_runtime`, `infra_runtime`, `nginx_config` 배포 효과를 분류한다.
- `backend_image` 배포는 backend 릴리즈 후보를 다시 검증하고 backend Docker 이미지를 Docker Hub에 발행한 뒤, EC2 `.env.prod`의 `BACKEND_IMAGE`만 새 태그로 바꾸고 backend를 pull/restart한다.
- `backend_runtime`과 `infra_runtime` 배포는 backend 이미지를 새로 만들지 않으며, staged compose/env preflight 후 필요한 runtime 파일과 서비스만 반영한다.
- 현재 기본 원칙은 "CI/CD 검증을 통과한 backend 고정 이미지 태그를 기준으로 하는 EC2 Docker Compose 릴리즈"다.

## Open Release TODOs

- Redis leaderboard snapshots written before the member surrogate-key rollout may contain legacy account-string members. After the first production release that writes the new numeric `active:v3` snapshot successfully, create and close a cleanup issue to expire/delete legacy leaderboard keys and confirm no reader falls back to them.
- JWT parsing currently keeps a temporary fallback from legacy `memberId` string claims to `account`. After all clients have refreshed onto tokens carrying numeric `memberId`, `account`, and `nickname`, create and close a cleanup issue to remove that fallback and fail legacy tokens explicitly.

## Non-negotiables

- 운영 환경 배포는 리뷰되지 않은 로컬 변경이나 미병합 브랜치 기준으로 하지 않는다.
- PR 브랜치명은 [docs/process/branch-and-pr-rules.md](docs/process/branch-and-pr-rules.md)의 `<type>/<kebab-case-summary>` 규칙을 반드시 통과해야 하며, `codex/*` 같은 자동화 접두사는 금지한다.
- 릴리즈 대상 커밋은 적어도 현재 기준 CI를 통과해야 한다.
- 프론트 변경이 있으면 `npm run build`를 기준 검증으로 삼는다.
- 백엔드 변경이 있으면 `./gradlew check`를 기준 검증으로 삼고, 구조 변경이 있으면 `./gradlew architectureLint`도 확인한다.
- 운영 배포는 `application-prod.yml`, repo의 `docker-compose.prod.yml`/`infra/`, 서버의 `.env.prod` 계약이 서로 맞는지 확인한 뒤 실행한다.
- 각 릴리즈는 하나의 고정된 commit SHA를 기준으로 식별한다.
- 릴리즈마다 변경 범위, 실행자, 대상 환경, 스모크 테스트 결과, 롤백 기준점을 기록한다.
- 환경 변수나 비밀값 계약이 바뀌면 [docs/release-docs/01-environments-and-artifacts.md](docs/release-docs/01-environments-and-artifacts.md)도 함께 갱신한다.
- DB 스키마나 API 계약을 깨는 변경은 무계획 단일 배포로 밀어 넣지 않는다.
- 장애 징후가 보이면 다음 롤아웃을 멈추고, 먼저 원인과 롤백 여부를 판단한다.

## Release Unit

이 저장소에서 릴리즈 단위는 아래처럼 정의한다.

- 코드 기준점: 하나의 commit SHA
- 범위 기준: frontend, backend, docs/config 변경 중 실제 반영 범위
- 운영 기록: 릴리즈 노트 또는 배포 로그 1건

프론트와 백엔드를 함께 배포하더라도, 운영 기록에서는 같은 릴리즈 ID 아래에 각 워크스페이스의 실제 반영 범위를 분리해서 적는다.

## Detailed Source Of Truth

배포/릴리즈의 상세 원문은 `RELEASE.md`가 아니라 `docs/release-docs/`다.

- 환경과 산출물: [01-environments-and-artifacts.md](docs/release-docs/01-environments-and-artifacts.md)
- 실행 체크리스트: [02-release-checklist.md](docs/release-docs/02-release-checklist.md)
- 롤아웃/롤백: [03-rollout-and-rollback.md](docs/release-docs/03-rollout-and-rollback.md)
- 프론트 Vercel 운영: [05-frontend-vercel-operations.md](docs/release-docs/05-frontend-vercel-operations.md)
- 기록 템플릿: [release-note-template.md](docs/release-docs/release-note-template.md)

운영 기준이 길어질수록 이 파일을 키우는 대신 `docs/release-docs/`를 갱신한다.

## Completion Checklist

배포/릴리즈 작업을 끝냈다고 보기 위한 최소 조건은 아래와 같다.

- 대상 커밋과 범위가 고정되었다.
- 필요한 검증이 모두 통과했다.
- 배포 순서와 롤백 기준을 확인했다.
- 스모크 테스트 또는 사후 확인을 수행했다.
- 릴리즈 기록을 남겼다.
