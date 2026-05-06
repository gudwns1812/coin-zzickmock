# 01. Environments And Artifacts

## Purpose

이 문서는 `coin-zzickmock`의 배포 대상 환경, 빌드 산출물, 설정값 계약, 릴리즈 기록 단위를 정의한다.
핵심 목표는 "어느 환경에 무엇을 배포하는지"와 "무엇을 릴리즈 완료의 증거로 남겨야 하는지"를 고정하는 것이다.

## Environment Model

이 저장소는 아래 환경 이름을 기준으로 사용한다.

### Local

- 목적: 개발과 수동 확인
- 소유: 각 개발자 로컬 머신
- 기본 검증: `npm run build`, `./gradlew check`
- 통합 인프라 확인: 루트 `docker compose up --build`
- 관측성 확인: [observability/local-infra-stack.md](observability/local-infra-stack.md)
- 비고: 운영 환경의 대체물이 아니라 사전 확인 단계다

### CI

- 목적: 병합 전 자동 검증
- 현재 구현: `.github/workflows/ci.yml`
- 기본 검증:
  - 프론트엔드 `npm run lint`, `npm run build`
  - 백엔드 `./gradlew check`
- 비고: 현재는 배포를 수행하지 않는다

### CD

- 목적: 고정된 릴리즈 후보를 운영 이미지와 운영 Compose 스택으로 승격
- 현재 구현: `.github/workflows/cd.yml`
- 기본 동작:
  - `main`/`master`의 `backend/**`, `docker-compose.prod.yml`, `infra/**` 변경 또는 수동 실행으로 시작한다
  - 백엔드 `./gradlew check`를 다시 수행한다
  - backend Docker 이미지를 Docker Hub에 push한다
  - SSH로 EC2에 접속해 repo의 `docker-compose.prod.yml`과 `infra/` 운영 설정을 복사한다
  - EC2에서 backend 이미지를 pull/restart한다
- 상세 기준: [04-production-cd.md](04-production-cd.md)

### Preview

- 목적: PR 단위 확인용 임시 환경
- 현재 구현 기준: frontend Vercel Preview Deployment
- 기본 backend 연결: `https://coin-zzickmock.duckdns.org`
- 상세 기준: [05-frontend-vercel-operations.md](05-frontend-vercel-operations.md)
- 최소 조건:
  - PR 또는 브랜치와 Vercel Preview URL이 연결되어야 한다
  - 운영 데이터에 영향을 줄 수 있는 write 검증은 테스트 계정으로 제한한다
  - 운영 비밀값을 클라이언트 번들로 노출하지 않는다

### Staging

- 목적: 운영 전 최종 검증
- 현재 상태: 아직 저장소 표준으로 고정되지 않음
- 도입 시 최소 조건:
  - 운영과 최대한 유사한 설정
  - 스모크 테스트와 롤백 리허설 가능
  - 릴리즈 후보 commit SHA를 명시 가능

### Production

- 목적: 실제 사용자 대상 환경
- 기본 원칙:
  - 검증된 commit SHA만 배포한다
  - 실행자와 배포 시각을 기록한다
  - 롤백 기준점이 없는 배포를 하지 않는다
  - frontend 운영 배포는 Vercel에서 담당한다
  - backend 운영 배포는 Docker Hub 이미지와 EC2 Docker Compose로 담당한다
- frontend backend 연결: `FUTURES_API_BASE_URL=https://coin-zzickmock.duckdns.org`

## Artifact Contract

릴리즈 후보는 아래 산출물 기준을 충족해야 한다.

### Frontend Artifact

- 기준 명령: 루트에서 `npm run build`
- 기준 워크스페이스: `frontend/`
- 배포 기준: Vercel
- 의미: Vercel에서 Next.js 프로덕션 빌드가 가능한 상태
- 기록 항목:
  - 대상 commit SHA
  - Vercel deployment URL
  - 빌드 성공 시각
  - 사용한 환경 변수 세트 이름

### Backend Artifact

- 기준 명령: `cd backend && ./gradlew check`
- 패키징 기준 명령: `cd backend && ./gradlew bootJar`
- 의미: 백엔드 검증과 실행 가능한 jar 산출 가능 상태
- 기록 항목:
  - 대상 commit SHA
  - 검증 결과
  - jar 생성 여부

### Production Docker Artifact

- 기준 워크플로: `.github/workflows/cd.yml`
- 기준 이미지:
  - `dockerhub-user/coin-zzickmock-backend:<tag>`
- 기준 플랫폼: `linux/arm64` Amazon Linux `aarch64`
- 운영 compose 기준:
  - `docker-compose.prod.yml`
  - `backend/src/main/resources/application-prod.yml`
  - `infra/prod.env.example`
- 의미: 운영 프로필과 Docker Compose로 backend, Redis, Nginx, Prometheus, Grafana, Loki를 실행 가능한 상태. Frontend는 이 Docker artifact에 포함하지 않고 Vercel에서 배포한다.

### Documentation And Config Artifact

- 설정값, 도메인, 비밀값 계약이 바뀌면 관련 문서도 릴리즈 산출물 일부로 본다.
- 최소 포함 문서:
  - [RELEASE.md](/Users/hj.park/projects/coin-zzickmock/RELEASE.md) 또는 상세 릴리즈 문서
  - 환경 계약이 바뀐 경우 관련 README나 운영 문서

### Local Infra Artifact

- 기준 명령: 루트에서 `docker compose up --build`
- 의미: 로컬에서 backend, MySQL, Redis, Nginx, Prometheus, Grafana, Loki를 함께 실행 가능해야 한다.
- 설정 위치:
  - `docker-compose.yml`
  - `infra/nginx/nginx.conf`
  - `infra/prometheus/prometheus.yml`
  - `infra/prometheus/alerts.yml`
  - `infra/grafana/`
  - `infra/loki/loki.yml`
  - `infra/promtail/promtail.yml`
- 기록 항목:
  - 사용한 commit SHA
  - 변경한 host port 또는 환경 변수
  - `docker compose ps`와 핵심 health/ready 확인 결과

## Environment Variable Policy

환경 변수는 "어디서 쓰이는지"와 "노출 가능 여부"가 분리되어야 한다.

### Frontend

- `NEXT_PUBLIC_*`는 공개 가능한 값만 넣는다.
- 비밀값은 프론트 클라이언트 번들로 노출하지 않는다.
- 현재 확인되는 프론트 변수:
  - `FUTURES_API_BASE_URL`: server-only backend base URL. production 값은 `https://coin-zzickmock.duckdns.org`
  - `NEXT_PUBLIC_BASE_URL`
  - `NEXT_PUBLIC_BASE_URL2`
  - `NEXT_PUBLIC_API_MOCKING`
- Vercel 환경 변수 계약과 Preview/Production 값은 [05-frontend-vercel-operations.md](05-frontend-vercel-operations.md)를 따른다.
- Next.js 환경 파일은 `frontend/` 아래에 둔다. 비밀 없는 기본값과 템플릿은 `frontend/.env.development`, `frontend/.env.test`, `frontend/.env.example`, `frontend/.env.preview.example`, `frontend/.env.production.example`로 관리하고, 실제 secret은 `.local` 파일 또는 Vercel env에만 둔다.

### Backend

- 백엔드 비밀값은 서버 전용 환경에만 둔다.
- 운영 자격증명은 코드, 샘플 파일, 문서 예시에 넣지 않는다.
- 설정값이 바뀌면 적용 대상 환경과 주입 위치를 릴리즈 기록에 남긴다.
- 운영 프로필은 `backend/src/main/resources/application-prod.yml`을 기준으로 하며, `MYSQL_*`, `REDIS_*`, `JWT_SECRET`을 서버 환경에서 주입한다.

### Production Docker Host

- 서버에는 비밀값을 담은 `.env.prod`가 있어야 한다.
- CD는 repo의 `docker-compose.prod.yml`과 `infra/` 운영 설정을 배포 때마다 서버로 복사한다.
- `.env.prod`의 공개 가능한 예시는 `infra/prod.env.example`에서 관리한다.
- CD의 EC2 SSH 사용자는 배포 경로 파일 반영을 위해 passwordless `sudo` 권한이 필요하며, Docker Compose는 `sudo` 없이 실행할 수 있어야 한다.
- GitHub Actions secret `EC2_SSH_PRIVATE_KEY`는 passphrase 없는 SSH private key 원문 전체를 실제 줄바꿈과 함께 저장해야 한다. `-----BEGIN ... PRIVATE KEY-----`/`-----END ... PRIVATE KEY-----` 경계를 포함하고, 터미널 프롬프트 문자나 zsh의 no-newline 표시인 `%` 같은 문자를 포함하지 않는다.
- Redis는 운영 compose 내부 서비스로 실행하고 host port를 공개하지 않는다.
- 운영 MySQL은 compose 내부에 포함하지 않고 `MYSQL_HOST` 또는 동등한 네트워크 경로로 연결한다.
- 프론트엔드 이미지, Vercel 변수, `NEXT_PUBLIC_*` 값은 EC2 Docker host 계약에 넣지 않는다.

### Shared Rule

- 어떤 값이 필요한지 이름과 용도는 문서화한다.
- 실제 비밀값 원문은 문서화하지 않는다.
- 새 환경 변수를 추가하면 배포 전에 "누가, 어느 환경에, 어떤 이름으로" 넣는지 명확해야 한다.
- 로컬 Compose 기본값은 `infra/local.env.example`에 공개 가능한 값만 둔다.

## Release Record Contract

각 릴리즈 기록은 최소 아래 항목을 가져야 한다.

- 릴리즈 ID 또는 제목
- 대상 환경
- 대상 commit SHA
- 포함 범위
- 실행자
- 실행 시각
- 사전 검증 결과
- 스모크 테스트 결과
- 롤백 기준점
- 후속 모니터링 또는 남은 리스크

기록 형식은 자유지만, 시작점은 [release-note-template.md](/Users/hj.park/projects/coin-zzickmock/docs/release-docs/release-note-template.md)를 권장한다.

## Maintenance Rule

- 새 배포 환경이 생기면 이 문서에 먼저 추가한다.
- 산출물 기준이 바뀌면 명령과 기록 항목을 함께 갱신한다.
- 실제 CD 파이프라인이 추가되면 현재 상태 설명을 최신화한다.
