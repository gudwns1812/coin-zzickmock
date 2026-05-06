# Local Infra Stack

이 문서는 로컬에서 `coin-zzickmock` 백엔드 런타임과 관측성 스택을 함께 띄우는 기준을 설명한다.
운영 배포 절차가 아니라 개발자 로컬 확인용 환경이다.

## Included Services

- `mysql`: backend 운영 DB 기본값과 같은 MySQL 계열 로컬 DB
- `redis`: cache, leaderboard, realtime support용 Redis
- `backend`: Spring Boot API, SSE, actuator/prometheus endpoint
- `nginx`: 외부 진입 reverse proxy
- `prometheus`: backend, nginx, Redis, Loki, Grafana metric scrape
- `grafana`: Prometheus와 Loki datasource, backend overview dashboard
- `loki`: 로컬 로그 저장소
- `promtail`: Docker container log 수집
- `nginx-exporter`, `redis-exporter`: proxy/cache infra metrics

## Commands

루트에서 실행한다.

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up --build -d
```

정리:

```bash
docker compose down
```

볼륨까지 삭제:

```bash
docker compose down -v
```

## Local URLs

- Backend health via Nginx: `http://localhost/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana via Nginx: `http://localhost/grafana/`
- Grafana direct local port: `http://localhost:3001/grafana/`
- Loki API: `http://localhost:3100`

Grafana local default account:

- user: `admin`
- password: `admin`

로컬 기본값을 바꿔야 하면 `infra/local.env.example`을 참고해서 루트 `.env`를 만든다.
실제 운영 비밀값은 문서나 샘플 파일에 넣지 않는다.

## Routing

Nginx는 `/api/futures/**`와 `/actuator/health`를 backend로 보내고, `/grafana/`를 Grafana로 보낸다.
프론트엔드 운영 배포는 Vercel이 담당하므로 이 Compose 스택에서 `/`는 제공하지 않는다.
`/actuator/prometheus`는 외부 Nginx 경로로 노출하지 않고 Prometheus가 Compose 내부 네트워크에서 직접 scrape한다.

SSE가 끊기지 않도록 backend API proxy는 buffering을 끄고 긴 read timeout을 사용한다.

## Observability

Prometheus 설정:

- `infra/prometheus/prometheus.yml`
- `infra/prometheus/alerts.yml`

Grafana provisioning:

- `infra/grafana/provisioning/datasources/datasources.yml`
- `infra/grafana/provisioning/dashboards/dashboards.yml`
- `infra/grafana/dashboards/backend-overview.json`

Loki/Promtail 설정:

- `infra/loki/loki.yml`
- `infra/promtail/promtail.yml`

Prometheus label과 Loki label은 `OBSERVABILITY.md`의 낮은 카디널리티 기준을 따른다.
회원 ID, 주문 ID, request ID, raw query string, email, phone number, token 원문은 metric label로 넣지 않는다.

## Verification

컨테이너가 떠 있는 상태에서 최소 확인:

```bash
docker compose ps
curl -fsS http://localhost/actuator/health
curl -fsS http://localhost:9090/-/ready
curl -fsS http://localhost:3100/ready
```

Grafana에서는 `Coin Zzickmock Backend Overview` dashboard가 자동으로 provision되어야 한다.
