# 04. Production CD

## Purpose

이 문서는 GitHub Actions backend CD와 EC2 Docker Compose 배포 계약을 정의한다.
목표는 backend 변경을 검증한 뒤 Docker Hub에 backend 이미지를 발행하고, repo의 compose/infra 설정을 EC2에 반영한 다음 해당 이미지를 pull/restart하는 것이다.

## Workflow

운영 CD 워크플로는 `.github/workflows/cd.yml`이다.

- `main`/`master`에 `backend/**`, `docker-compose.prod.yml`, `infra/**` 변경이 push되면 자동 실행한다.
- `workflow_dispatch`로 수동 실행할 수 있다.
- Frontend 변경만으로는 실행하지 않으며, frontend 배포는 Vercel에서 담당한다.

## Steps

1. `backend` Gradle check를 실행한다.
2. backend Docker image를 build한다.
3. Docker Hub에 push한다.
4. SSH로 EC2에 접속한다.
5. repo의 `docker-compose.prod.yml`과 `infra/` 운영 설정을 EC2의 `EC2_DEPLOY_PATH`로 복사한다.
6. EC2의 `EC2_DEPLOY_PATH`에서 서버 전용 `.env.prod`를 함께 사용한다.
7. 새 backend image를 pull한다.
8. backend container만 재시작한다.

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
- `EC2_USER`: EC2 SSH 사용자. 배포 경로 파일 반영과 Docker 명령을 위해 passwordless `sudo` 권한이 필요하다
- `EC2_SSH_PRIVATE_KEY`: EC2 접속용 private key
- `EC2_DEPLOY_PATH`: EC2에서 compose 파일과 `.env.prod`를 둔 디렉터리

## EC2 Server Files

EC2에는 `EC2_DEPLOY_PATH` 아래에 서버 전용 `.env.prod`를 둔다.
CD는 배포 시마다 repo의 `docker-compose.prod.yml`과 `infra/` 운영 설정을 같은 경로로 복사한다.

- `.env.prod`
- `docker-compose.prod.yml`
- `infra/nginx/`
- `infra/prometheus/`
- `infra/grafana/`
- `infra/loki/`
- `infra/promtail/`

`.env.prod`는 Git에 커밋하지 않고 CD에서도 덮어쓰지 않는다.
필요한 변수 이름과 공개 가능한 예시는 `infra/prod.env.example`을 따른다.

## Secrets Location

GitHub repository secrets에는 배포 접속과 Docker Hub 인증 정보를 둔다.
운영 runtime 비밀값은 EC2의 `.env.prod`에 둔다.

EC2 `.env.prod`에는 최소 아래 값이 필요하다.

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `JWT_SECRET`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`

CD 실행 시 `BACKEND_IMAGE`는 새 Docker Hub 이미지로 주입된다.
수동으로 EC2에서 compose를 실행할 때만 shell 환경에 `BACKEND_IMAGE`를 별도로 지정한다.

## Deploy Command

워크플로의 EC2 배포 단계는 아래 형태로 실행한다.

```bash
scp docker-compose.prod.yml <ec2>:/tmp/coin-zzickmock-<tag>/docker-compose.prod.yml
scp -r infra/nginx infra/prometheus infra/grafana infra/loki infra/promtail <ec2>:/tmp/coin-zzickmock-<tag>/infra/

sudo mkdir -p <EC2_DEPLOY_PATH>/infra
sudo cp /tmp/coin-zzickmock-<tag>/docker-compose.prod.yml <EC2_DEPLOY_PATH>/docker-compose.prod.yml
sudo cp -R /tmp/coin-zzickmock-<tag>/infra/* <EC2_DEPLOY_PATH>/infra/

sudo env BACKEND_IMAGE=<docker-hub-backend-image> \
  docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend

sudo env BACKEND_IMAGE=<docker-hub-backend-image> \
  docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps backend
```

배포 후에는 backend health와 Nginx proxy 상태를 릴리즈 기록에 남긴다.
