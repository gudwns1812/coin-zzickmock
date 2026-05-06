# 04. Production CD

## Purpose

이 문서는 GitHub Actions backend CD와 EC2 Docker Compose 배포 계약을 정의한다.
목표는 backend image 배포, backend runtime 설정 반영, non-backend infra 반영을 분리해 불필요한 image build/push와 backend 재시작을 피하는 것이다.

## Workflow

운영 CD 워크플로는 `.github/workflows/cd.yml`이다.

- `main`/`master`에 `backend/**`, `docker-compose.prod.yml`, `infra/**` 변경이 push되면 자동 실행한다.
- `workflow_dispatch`로 수동 실행할 수 있다.
- Frontend 변경만으로는 실행하지 않으며, frontend 배포는 Vercel에서 담당한다.

## Effect Model

CD는 먼저 배포 효과를 분류한다.

- `backend_image`: `backend/**` 변경 또는 수동 `backend-image`. Backend check를 실행하고 Docker Hub에 새 ARM64 image를 push한 뒤, EC2 `.env.prod`의 `BACKEND_IMAGE`만 새 태그로 바꾸고 backend를 pull/restart한다.
- `backend_runtime`: `docker-compose.prod.yml`의 `services.backend` 내부 변경 또는 수동 `backend-runtime`. 새 backend image를 만들지 않고 staged compose/env preflight 후 backend를 recreate한다.
- `infra_runtime`: `infra/prometheus/**`, `infra/grafana/**`, `infra/loki/**`, `infra/promtail/**`, `docker-compose.prod.yml`의 non-backend service 내부 변경, 또는 수동 `infra-runtime`. 새 backend image를 만들지 않고 non-backend 서비스만 pull/up한다.
- `nginx_config`: `infra/nginx/**` 변경. 새 backend image를 만들지 않고 nginx config를 sync한 뒤 실행 중인 nginx container에서 test/reload한다.

`docker-compose.prod.yml` 자동 분류는 확실한 `services.<name>` 내부 필드 변경만 통과한다. Top-level `name`, `networks`, `volumes`, 알 수 없는 top-level key, YAML anchor/alias/merge, service block add/remove/rename, deleted-only hunk, line mapping 실패는 ambiguous로 실패하며 수동 scope를 요구한다.

Nginx는 source에 따라 분리한다.

- `infra/nginx/**`: nginx config 변경. Runtime infra 서비스를 restart하지 않고, 실행 중인 nginx container에서 `nginx -t` 후 reload한다. Config-only 변경만으로 nginx를 recreate하지 않는다.
- `docker-compose.prod.yml`의 `services.nginx`: nginx service definition 변경. Staged preflight 후 nginx service를 recreate할 수 있다.

## Steps

1. `classify`가 배포 효과, `deploy`, `image_tag`를 계산한다.
2. `verify_backend_candidate`는 항상 job graph에 존재하지만, `backend_image=true`일 때만 backend `./gradlew check`를 실행한다.
3. `publish_backend_image`도 항상 job graph에 존재하지만, `backend_image=true`일 때만 Docker Hub login/build/push를 실행한다.
4. `deploy_to_ec2`는 `deploy=false` 또는 no-effect면 SSH, remote mutation, service operation, health check 없이 성공 summary만 남긴다.
5. 실제 deploy에서는 repo의 candidate compose/infra를 EC2 temp directory에 stage한다.
6. Live `.env.prod`에서 temp env를 만들고, `backend_image=true`일 때만 temp env의 `BACKEND_IMAGE`를 candidate image로 바꾼다.
7. `env -u BACKEND_IMAGE <compose> --env-file <temp-env> -f <candidate-compose> config --quiet`로 preflight한다. Backend-image-only 배포는 live compose와 candidate env를 preflight한다.
8. Preflight가 성공한 뒤에만 live 파일을 반영한다. Backend-image-only 배포는 live compose/infra를 바꾸지 않고 `.env.prod BACKEND_IMAGE`만 교체한다.
9. 분류된 효과에 맞는 서비스만 pull/up/reload한다.

## Image

CD는 아래 이미지를 발행한다.

- `<DOCKERHUB_USERNAME>/coin-zzickmock-backend:<tag>`

운영 EC2는 Amazon Linux `aarch64` 기준이므로 backend 이미지는 `linux/arm64` 플랫폼으로 build/push한다.
GitHub Actions의 amd64 runner에서도 같은 플랫폼 산출물을 만들기 위해 QEMU와 Docker Buildx를 사용한다.

이미지 tag는 수동 입력 `image_tag`가 있으면 그 값을 사용한다.
없으면 commit SHA 앞 7자리를 사용한다.

## GitHub Repository Secrets

GitHub repository secrets에 아래 secret을 둔다.
이 workflow는 repository secrets만 사용하도록 `environment: production`을 지정하지 않는다.
같은 이름의 environment secret이 repository secret을 덮어쓰는 혼선을 피하기 위함이다.

- `DOCKERHUB_USERNAME`: Docker Hub 사용자 또는 namespace
- `DOCKERHUB_TOKEN`: Docker Hub access token
- `EC2_HOST`: 운영 EC2 public host 또는 연결 가능한 host
- `EC2_SSH_PORT`: SSH port. 생략 시 workflow에서 `22`를 사용한다
- `EC2_USER`: EC2 SSH 사용자. 배포 경로 파일 반영을 위해 passwordless `sudo` 권한이 필요하며, Docker 명령은 `sudo` 없이 실행할 수 있어야 한다.
- `EC2_SSH_PRIVATE_KEY`: EC2 접속용 private key. 실제 줄바꿈을 유지한 private key 원문 전체를 저장해야 한다
- `EC2_DEPLOY_PATH`: EC2에서 compose 파일과 `.env.prod`를 둔 디렉터리

## EC2 Server Files

EC2에는 `EC2_DEPLOY_PATH` 아래에 서버 전용 `.env.prod`를 둔다.
CD는 runtime 파일 변경이 있는 배포에서만 repo의 `docker-compose.prod.yml`과 `infra/` 운영 설정을 같은 경로로 복사한다.

- `.env.prod`
- `docker-compose.prod.yml`
- `infra/nginx/`
- `infra/prometheus/`
- `infra/grafana/`
- `infra/loki/`
- `infra/promtail/`

`.env.prod`는 Git에 커밋하지 않는다. CD가 수정할 수 있는 값은 `^BACKEND_IMAGE=` 한 줄뿐이며, 다른 runtime 비밀값은 서버 소유 상태로 유지한다.
필요한 변수 이름과 공개 가능한 예시는 `infra/prod.env.example`을 따른다.

## Secrets Location

GitHub repository secrets에는 배포 접속과 Docker Hub 인증 정보를 둔다.
운영 runtime 비밀값과 현재 backend image reference는 EC2의 `.env.prod`에 둔다.

EC2 `.env.prod`에는 최소 아래 값이 필요하다.

- `BACKEND_IMAGE`: 현재 운영 backend image. 예: `dockerhub-user/coin-zzickmock-backend:<tag>`
- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `JWT_SECRET`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`
- `GRAFANA_ROOT_URL`: public Grafana URL including `/grafana/`, for example `http://coin-zzickmock.duckdns.org/grafana/`

CD는 compose 실행 때 shell-level `BACKEND_IMAGE`를 넘기지 않는다. 모든 compose command는 `env -u BACKEND_IMAGE`와 `--env-file .env.prod` 또는 staged temp env를 사용한다.

EC2에는 Docker Compose 실행기가 필요하다. CD는 `docker compose` v2 plugin을 먼저 사용하고, 없으면 legacy `docker-compose` binary를 사용한다. 둘 다 없으면 service operation 전에 중단한다.

## Deploy Scope

수동 실행의 `deploy_scope`는 아래 값을 사용한다.

- `backend-image`: 새 backend image를 build/push하고 `.env.prod BACKEND_IMAGE`를 교체한 뒤 backend를 pull/restart한다.
- `backend-runtime`: 새 image 없이 backend runtime 설정만 적용하고 backend를 recreate한다.
- `infra-runtime`: 새 image 없이 non-backend service만 적용한다.
- `all`: 세 효과를 모두 적용한다.
- `auto`: 현재 workflow SHA와 부모 commit의 diff를 기준으로 push와 같은 자동 분류를 수행한다.

`deploy=false`는 classify, backend verify, image publish까지 필요한 작업은 수행하되 SSH deploy, remote staging, `.env.prod` mutation, service pull/up/reload, health check를 모두 건너뛴다.
Comment/blank-only compose 변경처럼 모든 효과가 false인 no-effect run은 성공 summary만 남기고 원격 작업을 하지 않는다.

## Failure And Rollback

Staged preflight 실패는 live `docker-compose.prod.yml`, live `infra/`, live `.env.prod BACKEND_IMAGE`를 바꾸기 전에 workflow를 실패시킨다.

Backend recreate 후 health가 실패하면 workflow는 실패한다. 기본 정책은 automatic rollback 없음이다. 이미 시도된 backend image/env/runtime 상태를 그대로 두고, 운영자가 릴리즈 기록과 `docs/release-docs/03-rollout-and-rollback.md` 기준으로 수동 롤백 여부를 판단한다.

Nginx reload/recreate 실패도 workflow 실패로 보고한다. 이미 적용된 backend 또는 infra service 변경을 자동으로 되돌리지 않는다.

배포 후에는 backend health, Prometheus/Grafana 상태, Nginx proxy 상태를 릴리즈 기록에 남긴다.
