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
  - 백엔드 `./gradlew check :app:bootJar`
  - 백엔드 Docker image 포장 smoke build
- 비고: 현재는 배포를 수행하지 않는다

### CD

- 목적: 고정된 릴리즈 후보를 운영 이미지와 운영 Compose 스택으로 승격
- 현재 구현: `.github/workflows/cd.yml`
- 기본 동작:
  - `main`/`master`의 `backend/**`, `docker-compose.backend.prod.yml`, `docker-compose.infra.prod.yml`, `infra/**` 변경 또는 수동 실행으로 시작한다
  - 변경 범위를 `backend_image`, `backend_runtime`, `backend_agent_runtime`, `infra_runtime`, `nginx_config` 배포 효과로 분류한다
  - `backend_image`일 때만 백엔드 `./gradlew check :app:bootJar`와 backend Docker image push를 수행한다
  - backend host와 infra host에 각각 SSH 접속하며, 각 host에서 staged compose/env preflight를 먼저 수행한다
  - `backend_image`는 backend host `.env.prod`의 `BACKEND_IMAGE`만 새 태그로 교체한 뒤 backend를 pull/restart한다
  - `infra/prometheus/**`, `infra/grafana/**`, `infra/loki/**`, `docker-compose.infra.prod.yml` 변경은 infra host만 반영하고 backend host를 건드리지 않는다
  - `docker-compose.prod.yml`은 rollback anchor이며 정상 CD 범위에 포함하지 않는다
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
- 패키징 기준 명령: `cd backend && ./gradlew :app:bootJar`
- 의미: 백엔드 검증과 실행 가능한 jar 산출 가능 상태
- 기록 항목:
  - 대상 commit SHA
  - 검증 결과
  - `backend/app/build/libs/app.jar` 생성 여부

### Production Docker Artifact

- 기준 워크플로: `.github/workflows/cd.yml`
- 기준 이미지:
  - `dockerhub-user/coin-zzickmock-backend:<tag>`
- 기준 플랫폼: `linux/arm64` Amazon Linux `aarch64`
- 운영 compose 기준:
  - `docker-compose.backend.prod.yml`
  - `docker-compose.infra.prod.yml`
  - `backend/app/src/main/resources/application-prod.yml`
  - `infra/prod.env.example`
- rollback anchor:
  - `docker-compose.prod.yml`
- 의미: 운영 프로필과 host별 Docker Compose로 backend host의 backend/Nginx/telemetry agents와 infra host의 Redis/Prometheus/Grafana/Loki/exporter를 실행 가능한 상태. Frontend는 이 Docker artifact에 포함하지 않고 Vercel에서 배포한다. Host별 책임과 private port 계약은 [backend-infra-split-topology.md](backend-infra-split-topology.md)를 따른다.

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
- 운영 프로필은 `backend/app/src/main/resources/application-prod.yml`을 기준으로 하며, `MYSQL_*`, `REDIS_*`, `JWT_SECRET`을 서버 환경에서 주입한다. Split topology에서는 `REDIS_HOST`가 infra Redis private DNS/IP를 가리키고, `REDIS_PASSWORD`는 Redis auth/ACL을 켠 경우에만 서버 소유 secret으로 주입한다.
- Backend container resource 기본값은 Compose에서 `BACKEND_CPUS=2.0`, `BACKEND_MEMORY_LIMIT=1g`로 둔다.
  Infra/cache/observability container가 별도 host로 분리되므로 backend split compose에는 Redis/Prometheus/Grafana/Loki를 포함하지 않는다.
- `BACKEND_JAVA_TOOL_OPTIONS` 기본값은 `-XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError`다.
  JVM heap은 1GB container limit 기준으로 잡고 native memory 여유를 남긴다.
- 운영 backend는 별도 infra host의 Prometheus scrape를 위해 container `8080`을 host `${BACKEND_BIND_ADDRESS:-0.0.0.0}:${BACKEND_PORT:-8080}`로 publish한다.
  split topology에서는 scrape endpoint가 Nginx public route가 아니라 `http://<BACKEND_PRIVATE_HOST>:8080/actuator/prometheus`다.
  cloud security group에서는 TCP 8080 inbound를 `INFRA_EC2_HOST`에서 오는 트래픽으로 제한한다.
- Backend-owned auth cookie는 runtime 환경별로 `APP_AUTH_COOKIE_SECURE`, `APP_AUTH_COOKIE_SAME_SITE`를 명시한다.
  로컬 Compose 기본값은 HTTP 개발을 위해 `false`/`Lax`이고, 운영 Compose 기본값은 Vercel frontend에서 backend origin API 인증에 사용할 수 있도록 `true`/`None`이다.
- 시장 히스토리 repair queue/worker/retry 운영값은 `MARKET_HISTORY_REPAIR_*` 변수 묶음으로 조정한다. 이 값들은 비밀값이 아니며
  `application-prod.yml`, `docker-compose.backend.prod.yml`, `docker-compose.infra.prod.yml`, `infra/prod.env.example`의 계약을 함께 맞춘다.
- 커뮤니티 이미지 업로드는 backend-only S3 presign 설정으로 관리한다. Compose runtime에서는 `S3_BUCKET`, `S3_REGION`,
  `S3_KEY_PREFIX`, `COMMUNITY_IMAGE_ALLOWED_MIME`, `COMMUNITY_IMAGE_MAX_BYTES`, `COMMUNITY_IMAGE_PRESIGN_TTL`을 서버 환경에 주입하고,
  AWS 접근 권한은 인스턴스 역할이나 AWS SDK credential provider chain으로 공급한다. AWS access key/secret, presigned URL,
  업로드 credential 원문은 예시 파일·로그·릴리즈 기록에 남기지 않는다.
- 로컬 `docker-compose.yml`은 `.env`의 optional `AWS_PROFILE`을 backend 컨테이너에 전달하고, host `${HOME}/.aws`를
  `/home/app/.aws`로 read-only mount한다. 로컬에서 `aws configure` profile을 쓰면 `AWS_PROFILE`을 해당 profile 이름으로 맞춘다.
- 운영 compose에는 local AWS profile/access key 전달을 추가하지 않는다. 운영 S3 접근은 EC2 instance role
  또는 서버 전용 credential provider chain으로 공급하고, 운영 credential 원문은 `.env.prod` 예시나 릴리즈 문서에 기록하지 않는다.
- Backend는 `S3_BUCKET`과 `S3_REGION`으로 `https://{S3_BUCKET}.s3.{S3_REGION}.amazonaws.com` public URL prefix를 계산한다.
  compose runtime에는 위 S3 관련 환경 변수를 backend 컨테이너 환경으로 전달해야 한다.
- 커뮤니티 이미지 bucket/CORS는 프론트 배포 origin에서 `PUT`, `OPTIONS` preflight가 성공하도록 구성해야 한다.
  최소 허용 header는 `Content-Type`, `Content-MD5`, `x-amz-*`이며 임시 credential 구성이면 `x-amz-security-token`도 허용한다.

### Production Docker Host

- Backend host와 infra host에는 각각 비밀값을 담은 `.env.prod`가 있어야 한다.
- Backend host `.env.prod`에는 현재 운영 backend image를 가리키는 `BACKEND_IMAGE`가 있어야 한다.
- CD는 backend image 배포 때 backend host `.env.prod`의 `BACKEND_IMAGE` 한 줄만 새 Docker Hub 태그로 교체한다.
- CD는 backend-host runtime 변경 때 `docker-compose.backend.prod.yml`, `infra/nginx/`, `infra/promtail/`만 backend host로 복사한다.
- CD는 infra-host runtime 변경 때 `docker-compose.infra.prod.yml`, `infra/prometheus/`, `infra/grafana/`, `infra/loki/`, `infra/promtail/`만 infra host로 복사한다. Redis/Grafana/Prometheus/Loki 변경은 backend host를 건드리지 않는다.
- `.env.prod`의 공개 가능한 예시는 `infra/prod.env.example`에서 관리한다.
- CD의 EC2 SSH 사용자는 배포 경로 파일 반영을 위해 passwordless `sudo` 권한이 필요하며, Docker Compose는 `sudo` 없이 실행할 수 있어야 한다.
- GitHub Actions secret `EC2_SSH_PRIVATE_KEY`는 backend/infra host에 공통으로 쓰는 passphrase 없는 SSH private key 원문 전체를 실제 줄바꿈과 함께 저장해야 한다. Backend SSH user/path는 `EC2_USER`/`EC2_DEPLOY_PATH`, infra SSH user/path는 `INFRA_EC2_USER`/`INFRA_DEPLOY_PATH`를 사용한다. `-----BEGIN ... PRIVATE KEY-----`/`-----END ... PRIVATE KEY-----` 경계를 포함하고, 터미널 프롬프트 문자나 zsh의 no-newline 표시인 `%` 같은 문자를 포함하지 않는다.
- Redis는 infra host compose 내부 서비스로 실행하고 backend host에는 포함하지 않는다. Redis host port는 infra private interface로만 bind한다.
- Backend host port 8080은 별도 infra host Prometheus scrape 전용 direct/private endpoint다. Public internet 전체에 열지 말고, Nginx를 경유하지 않는 `INFRA_EC2_HOST` source rule로만 제한한다.
- 운영 MySQL은 compose 내부에 포함하지 않고 `MYSQL_HOST` 또는 동등한 네트워크 경로로 연결한다.
- Split infra Grafana는 backend Nginx `/grafana/` 경로 뒤에 두지 않고 `http://<INFRA_BIND_ADDRESS>:<GRAFANA_PORT>/`로 직접 접속한다. `GRAFANA_ROOT_URL`/subpath 설정은 split infra compose에서 쓰지 않는다.
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
