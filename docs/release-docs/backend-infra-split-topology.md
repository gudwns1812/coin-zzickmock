# Backend/Infra Split Topology

## Purpose

This runbook fixes the first production topology split between the backend runtime host and the infra/cache/observability host. It is an execution guide for deployment boundaries, private traffic, migration, validation, and rollback. It does not choose instance size, move MySQL, introduce managed services, add HA/autoscaling, or change backend/frontend application behavior.

## Non-goals

- No t4g.micro/t4g.small or other instance-size recommendation.
- No MySQL/DB relocation.
- No ElastiCache, managed Grafana/Prometheus, AMP, or other managed-service conversion.
- No HA, multi-AZ, autoscaling, multi-backend, or failover design.
- No backend/frontend application behavior change.

## Decision

Use a two-host topology for the first split:

- Backend host: backend application container, Nginx public edge, and backend-host-local telemetry agents/exporters.
- Infra host: Redis plus central Prometheus, Grafana, Loki, Redis exporter, infra node exporter, and optional infra-host promtail.

The backend host remains the only public application ingress for ports 80/443. Observability tools that access backend internals must use direct private backend endpoints and must not scrape backend metrics through Nginx.

## Service Groups

### Backend host

- `backend`: Spring Boot application runtime.
- `nginx`: public HTTPS edge for API, `/actuator/health`, and the operator Grafana subpath proxy if the existing public Grafana route is retained.
- `backend-promtail`: backend-host-local Docker log collector that pushes to infra Loki.
- `backend-node-exporter`: backend host metrics, scraped by infra Prometheus over private networking.
- `nginx-exporter`: Nginx status exporter, scraped by infra Prometheus over private networking.

### Infra host

- `redis`: backend cache/queue endpoint.
- `prometheus`: central scrape, rule, and alert evaluation.
- `grafana`: central dashboards. Direct port is private only.
- `loki`: central log store/ingestion. Direct port is private only.
- `redis-exporter`: Redis metrics.
- `infra-node-exporter`: infra host metrics.
- `infra-promtail`: optional infra-host-local log collector.

## Private Traffic Matrix

| Flow | Source | Destination | Port/path | Exposure rule | Validation |
| --- | --- | --- | --- | --- | --- |
| Public API and health | Internet | Backend-host Nginx | 80/443, `/api/futures/**`, `/actuator/health` | Public only on backend host | Public health/API smoke |
| Backend Redis client | Backend host/container | Infra-host Redis | tcp 6379 | Infra private interface/security group only | Redis ping and backend Redis health |
| Backend metrics scrape | Infra-host Prometheus | Backend private endpoint | tcp 8080, `/actuator/prometheus` | Backend private interface/security group only; not Nginx | `curl -fsS http://<backend-private-host>:8080/actuator/prometheus` from infra host |
| Backend host metrics scrape | Infra-host Prometheus | Backend-host node exporter | tcp 9100 | Backend private interface/security group only | Prometheus target up |
| Nginx metrics scrape | Infra-host Prometheus | Backend-host nginx exporter | tcp 9113 | Backend private interface/security group only | Prometheus target up |
| Backend logs push | Backend-host promtail | Infra-host Loki | tcp 3100, `/loki/api/v1/push` | Infra private interface/security group only | Loki receives backend-host log stream |
| Redis metrics scrape | Infra-host Prometheus | Infra-host redis exporter | tcp 9121 | Infra-local or infra private only | Prometheus target up |
| Grafana operator UI | Operator via current public route or private operator path | Grafana | `/grafana/` through backend Nginx if retained, or direct private operator access in a later change | Public UI path is an operator surface only; it is not a backend metrics scrape path | Grafana subpath/private UI check |

Direct/private means reachable from the infra host or approved operator network, not open to the public internet. Security groups must restrict backend tcp 8080, 9100, and 9113 to the infra host. Nginx must continue to keep non-health `/actuator/**` requests out of the public route set.

## Runtime Configuration Contract

Backend host `.env.prod`:

- `REDIS_HOST`: infra Redis private DNS/IP.
- `REDIS_PORT`: `6379` unless the infra Redis endpoint differs.
- `REDIS_PASSWORD`: server-owned Redis password or ACL secret when Redis leaves compose-internal networking.
- `BACKEND_PORT`: host port for direct/private backend scrape, default `8080`.
- `BACKEND_BIND_ADDRESS`: optional host bind address for backend `8080`, default `0.0.0.0`; restrict public exposure with the cloud security group.
- `GRAFANA_PRIVATE_HOST`: optional infra Grafana private DNS/IP; default is `REDIS_HOST`.
- `LOKI_PUSH_URL`: optional infra Loki push URL for backend-host promtail; default is `http://<REDIS_HOST>:3100/loki/api/v1/push`.

Infra host `.env.prod`:

- `INFRA_BIND_ADDRESS`: infra private interface used by Redis, Prometheus, Grafana, and Loki. Restrict inbound rules by source.
- `BACKEND_PRIVATE_HOST`: backend private DNS/IP used by infra Prometheus `backend-private` extra host.

GitHub Actions secrets for split CD:

- `EC2_HOST`, `EC2_USER`, `EC2_DEPLOY_PATH`: backend host address, backend SSH user, and backend deployment path.
- `EC2_SSH_PORT`, `EC2_SSH_PRIVATE_KEY`: shared SSH port and key reused for both backend and infra hosts.
- `INFRA_EC2_HOST`, `INFRA_EC2_USER`, `INFRA_DEPLOY_PATH`: infra host address, infra SSH user, and infra deployment path.
- `EC2_BACKEND_METRICS_HOST`: backend private DNS/IP used by infra host for `http://<host>:8080/actuator/prometheus`. Backend-host CD deploy requires this value; it must be a private backend value rather than the public Nginx host.

Prometheus must scrape backend application metrics from `http://<backend-private-host>:8080/actuator/prometheus`, not from `https://<public-domain>/actuator/prometheus` and not from Nginx.

## CD Scope Contract

The CD workflow deploys split host scopes through separate compose files and separate host jobs:

- `docker-compose.backend.prod.yml` is the backend host contract. It contains backend, Nginx, backend-host promtail, nginx exporter, and backend node exporter. It does not contain Redis, Prometheus, Grafana, or Loki.
- `docker-compose.infra.prod.yml` is the infra host contract. It contains Redis, Prometheus, Grafana, Loki, infra-host promtail, Redis exporter, and infra node exporter. It does not contain backend or Nginx.
- `docker-compose.prod.yml` is a colocated rollback anchor and is not deployed by normal CD.

Deployment effects:

- `backend_image`: build/publish backend image, mutate backend host `.env.prod` `BACKEND_IMAGE`, recreate backend only, verify backend health and direct metrics reachability. It must not restart central infra services.
- `backend_runtime`: apply backend host compose/env/runtime changes and recreate backend only. It must not publish an image or restart infra services.
- `backend_agent_runtime`: apply backend-host-local promtail/exporter runtime only. It must not recreate the backend application.
- `nginx_config`: apply backend host Nginx config, run `nginx -t`, reload/recreate Nginx according to config vs service-definition scope. It must not restart backend unless paired with a backend effect.
- `infra_runtime`: apply infra host Redis/Prometheus/Grafana/Loki/exporter/storage changes and verify Redis, Prometheus, Grafana, and Loki. It must not build/publish/recreate backend.

Path classification must preserve this boundary: `infra/prometheus/**`, `infra/grafana/**`, `infra/loki/**`, and `docker-compose.infra.prod.yml` are infra-host-only effects. `infra/promtail/**` is shared agent config and may touch backend-host promtail, but it still must not recreate backend app runtime.

Every two-host CD run must report which host(s) were touched and which host(s) were intentionally not touched.

## Migration Sequence

1. Snapshot current colocated state: commit SHA, backend image tag, live compose, live env, infra config, `docker compose ps`, Redis persistence snapshot, and Grafana/Prometheus/Loki volume state.
2. Prepare private network/security rules: backend 80/443 public, backend 8080/9100/9113 from infra only, infra 6379/3000/3100/9121 private only, SSH restricted to operator sources.
3. Provision infra host services: Redis, Prometheus, Grafana, Loki, Redis exporter, infra node exporter, and optional infra promtail.
4. Inventory Redis keys as durable-ish queue/state, reconstructable cache, or disposable transient data. Default to no-dual-writes cutover with backend stopped or write-paused during Redis snapshot/copy. Keep old Redis data through validation.
5. Validate private connectivity before switching backend: backend host to infra Redis, infra host to backend `:8080/actuator/prometheus`, infra Prometheus to backend node/nginx exporters, backend promtail to infra Loki, and backend Nginx to private Grafana if the public Grafana subpath is retained.
6. Switch backend host runtime targets: `REDIS_HOST`, `REDIS_PASSWORD`, `BACKEND_PORT`/optional bind override, Redis target, and optional Grafana/Loki overrides if defaults do not match. Recreate or reload only affected backend-host services.
7. Switch Prometheus scrape targets to backend private host/exporters and infra-local services.
8. Run the validation window: public API/health smoke, Redis-backed smoke, direct metrics scrape, Prometheus target states, Loki backend logs, Grafana dashboards, 5xx, p95/p99, Redis errors, host CPU/memory.
9. Disable old colocated infra services only after validation. Keep old volumes/config snapshots until rollback retention expires.

## Rollback

Rollback triggers:

- Backend health or public API smoke fails after Redis/direct endpoint switch.
- Redis connection errors, queue failures, or critical Redis-backed flow failures appear after switch.
- Direct backend metrics from infra host fail and the rollout requires observability continuity.
- Prometheus cannot scrape backend critical targets due to split config.
- Loki receives no backend-host logs after promtail switch and observability loss is unacceptable.
- Nginx cannot proxy public API or retained Grafana route correctly.
- 5xx or p95/p99 regress materially from the pre-cutover baseline.

Rollback path:

1. Stop or write-pause backend before Redis rollback unless the Redis key inventory proves affected keys are reconstructable or disposable.
2. Restore backend host `.env.prod`, compose, Nginx, promtail/exporter config snapshots from before cutover.
3. Ensure old colocated Redis is running with preserved volume/AOF/RDB.
4. Recreate backend with old Redis target and previous backend bind settings.
5. Test backend health locally and through Nginx, then run at least one Redis-backed smoke flow.
6. Restore old Prometheus/Loki/Grafana colocated services if observability rollback is needed.
7. If only Grafana public route fails and backend app is healthy, roll back Nginx/Grafana proxy config first; do not automatically move Redis back.
8. If only Prometheus targets fail and backend app is healthy, roll back Prometheus/observability config only.
9. Record rollback result and keep infra host data until divergence is understood.

## Acceptance Checklist

- Backend-to-Redis, Prometheus-to-backend, Promtail-to-Loki, Grafana exposure, and exporter flows are named with source, destination, port/path, exposure, and validation.
- Observability tools access backend metrics through direct/private backend endpoints, not Nginx public actuator routes.
- Direct backend tcp 8080 is restricted to the infra host or approved private/operator sources.
- CD behavior separates backend image, backend runtime, infra runtime, Nginx/proxy config, and backend-host-local infra agents.
- Migration includes infra prep, connectivity validation, backend target switch, health/metrics/log validation, and delayed removal of colocated infra.
- Rollback includes triggers, config/env restore targets, service restart order, and validation checks.
- Scope exclusions remain intact: no instance sizing recommendation, no MySQL move, no managed-service conversion, no HA/autoscaling, and no application behavior change.
