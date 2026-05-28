# 04. Production CD

## Purpose

이 문서는 GitHub Actions backend/infra CD와 EC2 Docker Compose 배포 계약을 정의한다.
목표는 backend image 배포, backend-host runtime, infra-host runtime, Nginx config 반영을 분리해 Redis, Grafana, Prometheus, Loki 변경이 backend build/publish/recreate를 유발하지 않도록 고정하는 것이다.

## Workflow

운영 CD 워크플로는 `.github/workflows/cd.yml`이다.

- `main`/`master`에 `backend/**`, `docker-compose.backend.prod.yml`, `docker-compose.infra.prod.yml`, `infra/**` 변경이 push되면 자동 실행한다.
- `workflow_dispatch`로 수동 실행할 수 있다.
- Frontend 변경만으로는 실행하지 않으며, frontend 배포는 Vercel에서 담당한다.
- Legacy `docker-compose.prod.yml`은 rollback anchor다. 이 파일만 바꾸는 push는 CD를 시작하지 않는다. 다른 CD 대상 파일과 함께 변경되어 workflow가 실행되면 rollback-anchor warning만 남기고 배포 효과로 분류하지 않는다.

## Split Compose Contract

운영 split topology는 host별 compose 파일을 분리한다.

- `docker-compose.backend.prod.yml`: backend app, public Nginx, backend-host promtail, nginx exporter, backend node exporter만 소유한다. Redis, Prometheus, Grafana, Loki는 포함하지 않는다.
- `docker-compose.infra.prod.yml`: Redis, Prometheus, Grafana, Loki, infra-host promtail, Redis exporter, infra node exporter만 소유한다. Backend app과 Nginx는 포함하지 않는다.
- `docker-compose.prod.yml`: 기존 colocated 배포 복구용 rollback anchor다. 정상 CD 범위에 포함하지 않는다.

서버별 `.env.prod`는 Git에 커밋하지 않는다. 공개 가능한 변수 이름과 예시는 `infra/prod.env.example`을 따른다.

## Effect Model

CD는 먼저 배포 효과를 분류한다. Backend/infra split host ownership은 [backend-infra-split-topology.md](backend-infra-split-topology.md)의 CD scope contract를 따른다.

- `backend_image`: `backend/**` 변경 또는 수동 `backend-image`. Backend check를 실행하고 Docker Hub에 새 ARM64 image를 push한 뒤, backend host `.env.prod`의 `BACKEND_IMAGE`만 새 태그로 바꾸고 backend만 pull/restart한다. Infra host는 건드리지 않는다.
- `backend_runtime`: `docker-compose.backend.prod.yml` 변경 또는 수동 `backend-runtime`. 새 backend image를 만들지 않고 backend host compose/env preflight 후 backend만 recreate한다. Infra host는 건드리지 않는다.
- `backend_agent_runtime`: `infra/promtail/**` 또는 backend-host telemetry agent service definition 변경. Backend app image를 만들거나 backend app을 recreate하지 않고 backend-host agent/exporter만 반영한다.
- `infra_runtime`: `docker-compose.infra.prod.yml`, `infra/prometheus/**`, `infra/grafana/**`, `infra/loki/**`, `infra/promtail/**` 변경 또는 수동 `infra-runtime`. 새 backend image를 만들지 않고 infra host의 Redis/Prometheus/Grafana/Loki/exporter/promtail 서비스만 pull/up한다. Backend host app/Nginx는 건드리지 않는다. `infra/promtail/**`은 shared agent config라 backend-host promtail도 함께 반영할 수 있지만 backend app runtime은 건드리지 않는다.
- `nginx_config`: `infra/nginx/**` 변경 또는 수동 `nginx-config`. 새 backend image를 만들지 않고 backend host Nginx config만 sync/test/reload한다.
- `nginx_service_definition`: `docker-compose.backend.prod.yml`의 Nginx service definition 변경. Staged preflight 후 Nginx service를 recreate할 수 있다.

자동 path classification은 아래처럼 고정한다.

| Changed path | Effects |
| --- | --- |
| `backend/**` | `backend_image` |
| `docker-compose.backend.prod.yml` | `backend_runtime`, `backend_agent_runtime`, `nginx_service_definition` |
| `docker-compose.infra.prod.yml` | `infra_runtime` |
| `infra/prometheus/**`, `infra/grafana/**`, `infra/loki/**` | `infra_runtime` only |
| `infra/promtail/**` | `infra_runtime`, `backend_agent_runtime` |
| `infra/nginx/**` | `nginx_config` |
| `docker-compose.prod.yml` | no deploy effect; rollback-anchor warning only when the workflow is already running for another matched path |

따라서 Redis, Grafana, Prometheus, Loki 설정 변경은 backend CD가 아니라 infra-host CD만 실행한다.

## Steps

1. `classify`가 배포 효과, `deploy`, `image_tag`, `backend_host_effect`, `infra_host_effect`를 계산한다.
2. `verify_backend_candidate`는 항상 job graph에 존재하지만, `backend_image=true`일 때만 backend `./gradlew check :app:bootJar`를 실행하고 executable jar를 workflow artifact로 업로드한다.
3. `publish_backend_image`도 항상 job graph에 존재하지만, `backend_image=true`일 때만 jar artifact를 `backend/app/build/libs/`에 내려받아 runtime Docker image로 포장한 뒤 Docker Hub login/build/push를 실행한다.
4. `deploy_infra_host`는 `infra_host_effect=true`일 때만 infra host에 SSH 접속한다. Infra-host deploy는 `docker-compose.infra.prod.yml`, `infra/prometheus/`, `infra/grafana/`, `infra/loki/`, `infra/promtail/`만 stage한다.
   Live `infra/*` config directories are updated in place so existing Docker bind mounts do not keep pointing at an unlinked directory. File-mounted infra services (`loki`, `promtail`, `prometheus`, `grafana`) are force-recreated after config promotion so provisioned dashboards and config files are visible inside the running containers.
5. `deploy_backend_host`는 `backend_host_effect=true`일 때만 backend host에 SSH 접속한다. Backend-host deploy는 `docker-compose.backend.prod.yml`, `infra/nginx/`, `infra/promtail/`만 stage한다. 같은 workflow run에서 infra/backend host 효과가 함께 있으면 backend-host deploy는 infra-host job 완료 후 진행해 Redis/Loki direct dependency race를 피한다.
6. 각 host는 live `.env.prod`에서 candidate env를 만들고 staged compose/env preflight가 성공한 뒤에만 live 파일을 반영한다.
7. `backend_image=true`일 때만 backend host `.env.prod BACKEND_IMAGE`를 candidate image로 교체한다. 다른 runtime 배포는 `.env.prod` 비밀값이나 image tag를 바꾸지 않는다.
8. 분류된 효과에 맞는 서비스만 pull/up/reload한다.
9. Backend host 효과가 있는 배포 후에는 infra host에서 backend private metrics endpoint로 direct scrape reachability를 검증한다. 이 검증은 Nginx public route를 사용하지 않는다.
10. Infra host 효과가 있는 배포 후에는 infra host 안에서 Prometheus와 Loki readiness를 확인하고, Grafana container가 updated provisioned dashboard file을 실제 bind mount 경로에서 보는지 확인한다. Backend host는 건드리지 않는다.

## Image

CD는 아래 이미지를 발행한다.

- `<DOCKERHUB_USERNAME>/coin-zzickmock-backend:<tag>`

운영 EC2는 Amazon Linux `aarch64` 기준이므로 backend 이미지는 `linux/arm64` 플랫폼으로 build/push한다.
GitHub Actions의 amd64 runner에서도 같은 플랫폼 산출물을 만들기 위해 QEMU와 Docker Buildx를 사용한다.
Executable jar는 Docker build 내부가 아니라 GitHub Actions Gradle 환경에서 먼저 만든다.
CD는 Dockerfile의 `jar-runtime` target을 사용해 `backend/app/build/libs/app.jar`를 runtime image에 복사하는 포장 단계만 수행하며, Docker build 안에서 Gradle을 실행하지 않는다.
로컬 Compose는 개발 편의를 위해 `source-runtime` target을 사용하고 Docker build 안에서 source jar를 만든다.

이미지 tag는 수동 입력 `image_tag`가 있으면 그 값을 사용한다.
없으면 commit SHA 앞 7자리를 사용한다.

## GitHub Repository Secrets

GitHub repository secrets에 아래 secret을 둔다.
이 workflow는 repository secrets만 사용하도록 `environment: production`을 지정하지 않는다.
같은 이름의 environment secret이 repository secret을 덮어쓰는 혼선을 피하기 위함이다.

공통 image publish secret:

- `DOCKERHUB_USERNAME`: Docker Hub 사용자 또는 namespace
- `DOCKERHUB_TOKEN`: Docker Hub access token

Backend host deploy secret:

- `EC2_HOST`: backend host public SSH host 또는 접근 가능한 host
- `EC2_SSH_PORT`: SSH port. 생략 시 workflow에서 `22`를 사용한다
- `EC2_USER`: backend host SSH 사용자. 배포 경로 파일 반영을 위해 passwordless `sudo` 권한이 필요하다. Docker 명령은 `sudo` 없이 실행 가능하거나 `sudo -n docker ...`로 실행 가능해야 한다.
- `EC2_SSH_PRIVATE_KEY`: backend host 접속용 private key. 실제 줄바꿈을 유지한 private key 원문 전체를 저장해야 한다
- `EC2_DEPLOY_PATH`: backend host에서 `docker-compose.backend.prod.yml`, `.env.prod`, `infra/nginx/`, `infra/promtail/`을 둔 디렉터리

Infra host deploy and verification secret:

- `INFRA_EC2_HOST`: infra host public SSH host 또는 접근 가능한 host. SSH port/key는 기존 `EC2_SSH_PORT`, `EC2_SSH_PRIVATE_KEY`를 그대로 재사용한다.
- `INFRA_EC2_USER`: infra host SSH 사용자. 현재 infra host가 Ubuntu 계열이면 보통 `ubuntu`다.
- `INFRA_DEPLOY_PATH`: infra host에서 `docker-compose.infra.prod.yml`, `.env.prod`, `infra/prometheus/`, `infra/grafana/`, `infra/loki/`, `infra/promtail/`을 둔 디렉터리
- `EC2_BACKEND_METRICS_HOST`: infra host에서 접근할 backend private DNS/IP. Backend-host deploy 후 direct metrics reachability 검증에 사용한다. Public Nginx host를 넣지 않는다

## Server Files

Backend host에는 `EC2_DEPLOY_PATH` 아래에 아래 파일을 둔다.

- `.env.prod`
- `docker-compose.backend.prod.yml`
- `infra/nginx/`
- `infra/promtail/`

Infra host에는 `INFRA_DEPLOY_PATH` 아래에 아래 파일을 둔다.

- `.env.prod`
- `docker-compose.infra.prod.yml`
- `infra/prometheus/`
- `infra/grafana/`
- `infra/loki/`
- `infra/promtail/`

`.env.prod`는 Git에 커밋하지 않는다. CD가 backend host `.env.prod`에서 수정할 수 있는 값은 `^BACKEND_IMAGE=` 한 줄뿐이며, 다른 runtime 비밀값은 서버 소유 상태로 유지한다.

## Secrets Location

GitHub repository secrets에는 배포 접속과 Docker Hub 인증 정보를 둔다.
운영 runtime 비밀값과 현재 backend image reference는 각 EC2 host의 `.env.prod`에 둔다.

Backend host `.env.prod`에는 최소 아래 값이 필요하다.

- `BACKEND_IMAGE`: 현재 운영 backend image. 예: `dockerhub-user/coin-zzickmock-backend:<tag>`
- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
- `JWT_SECRET`
- `REDIS_HOST`: infra Redis private DNS/IP
- `REDIS_PORT`, `REDIS_DATABASE`
- `REDIS_PASSWORD`: 선택값. Redis auth/ACL을 켠 경우에만 설정한다
- `BACKEND_PORT`: direct/private backend scrape port, 기본 `8080`
- `BACKEND_BIND_ADDRESS`: 선택값. backend host에서 8080을 bind할 주소이며 기본은 `0.0.0.0`이다. public 노출은 security group으로 막는다
- `GRAFANA_PRIVATE_HOST`: 선택값. 지정하지 않으면 `REDIS_HOST`를 infra Grafana host로 쓴다
- `LOKI_PUSH_URL`: 선택값. 지정하지 않으면 `http://<REDIS_HOST>:3100/loki/api/v1/push`를 쓴다

Infra host `.env.prod`에는 최소 아래 값이 필요하다.

- `INFRA_BIND_ADDRESS`: Redis, Prometheus, Grafana, Loki를 bind할 infra private interface
- `BACKEND_PRIVATE_HOST`: infra Prometheus가 scrape할 backend private DNS/IP
- `PROMETHEUS_PORT`
- `GRAFANA_PORT`, `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`
- Grafana UI는 backend Nginx subpath가 아니라 `http://<INFRA_BIND_ADDRESS>:<GRAFANA_PORT>/`로 직접 접속한다.
- `LOKI_PORT`
- `REDIS_PASSWORD`: 선택값. Redis auth/ACL을 켠 경우에만 설정한다

CD는 compose 실행 때 shell-level `BACKEND_IMAGE`를 넘기지 않는다. 모든 compose command는 `env -u BACKEND_IMAGE`와 `--env-file .env.prod` 또는 staged temp env를 사용한다.

EC2에는 Docker Compose 실행기가 필요하다. CD는 먼저 SSH user의 직접 `docker` 접근을 시도하고, Docker socket 권한이 없으면 passwordless `sudo -n docker`로 fallback한다. Compose 실행기는 `docker compose` v2 plugin을 먼저 사용하고, 없으면 legacy `docker-compose` binary를 사용한다. 둘 다 없거나 Docker가 직접/`sudo -n` 어느 쪽으로도 실행되지 않으면 service operation 전에 중단한다.

## Infra Prometheus Scrape Contract

운영 backend compose는 Spring Boot actuator metrics를 별도 infra host Prometheus가 가져갈 수 있도록 backend container `8080`을 host `${BACKEND_BIND_ADDRESS:-0.0.0.0}:${BACKEND_PORT:-8080}`로 publish한다. 이 경로는 관측 도구 전용 direct/private access이며 Nginx를 경유하지 않는다.

- Scrape URL: `http://<EC2_BACKEND_METRICS_HOST>:8080/actuator/prometheus`
- GitHub secret: `EC2_BACKEND_METRICS_HOST`에는 infra host에서 접근 가능한 backend private DNS/IP를 둔다. Public Nginx host 대신 private host 값을 명시한다.
- Prometheus config: `infra/prometheus/prometheus.prod.yml`은 `backend-private:8080`, `backend-private:9100`, `backend-private:9113`을 scrape하며, infra compose의 `extra_hosts`가 `backend-private`을 `BACKEND_PRIVATE_HOST`로 해석한다.
- CD 검증: backend-host deploy 후 GitHub Actions가 `INFRA_EC2_HOST`에 SSH로 접속해 위 scrape URL을 `curl -fsS`로 확인한다. 검증은 `https://.../actuator/*` 또는 Nginx public domain을 사용하지 않는다.
- Network rule: backend host security group은 TCP 8080, 9100, 9113 inbound를 infra host source로 제한해야 한다. 이 repository는 security group을 직접 변경하지 않는다.

## Deploy Scope

수동 실행의 `deploy_scope`는 아래 값을 사용한다.

- `backend-image`: 새 backend image를 build/push하고 backend host `.env.prod BACKEND_IMAGE`를 교체한 뒤 backend를 pull/restart한다.
- `backend-runtime`: 새 image 없이 backend host compose/runtime 설정만 적용하고 backend를 recreate한다.
- `backend-agent-runtime`: backend-host promtail/nginx-exporter/node-exporter runtime만 적용한다. Backend app은 recreate하지 않는다.
- `infra-runtime`: 새 image 없이 infra host Redis/Prometheus/Grafana/Loki/exporter/promtail service만 적용한다. Backend host는 건드리지 않는다.
- `nginx-config`: backend host Nginx config만 sync/test/reload한다.
- `all`: backend image, backend runtime, backend agent runtime, infra runtime, nginx config를 모두 적용한다.
- `auto`: 현재 workflow SHA와 부모 commit의 diff를 기준으로 push와 같은 자동 분류를 수행한다.

`deploy=false`는 classify, backend verify, image publish까지 필요한 작업은 수행하되 SSH deploy, remote staging, `.env.prod` mutation, service pull/up/reload, health check를 모두 건너뛴다.
No-effect run은 성공 summary만 남기고 원격 작업을 하지 않는다.

## Failure And Rollback

Staged preflight 실패는 live compose, live infra config, live `.env.prod BACKEND_IMAGE`를 바꾸기 전에 workflow를 실패시킨다.

Backend recreate 후 health가 실패하면 workflow는 실패한다. 기본 정책은 automatic rollback 없음이다. 이미 시도된 backend image/env/runtime 상태를 그대로 두고, 운영자가 릴리즈 기록과 [03-rollout-and-rollback.md](03-rollout-and-rollback.md) 기준으로 수동 롤백 여부를 판단한다.

Nginx reload/recreate 실패도 workflow 실패로 보고한다. 이미 적용된 backend 또는 infra service 변경을 자동으로 되돌리지 않는다.

Infra service readiness 실패는 infra-host deploy 실패로 보고한다. 같은 run에 backend-host 효과도 함께 있으면 backend-host deploy는 시작하지 않는다. Infra-only 변경은 backend image를 만들거나 backend app을 재시작하지 않는다.

배포 후에는 실제 touched host, intentionally untouched host, backend health, direct backend metrics scrape 결과, Prometheus/Grafana/Loki 상태, Nginx proxy 상태를 릴리즈 기록에 남긴다. Direct metrics scrape 실패는 Nginx 경로로 우회해 성공 처리하지 않는다.
