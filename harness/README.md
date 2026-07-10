# Java SDK Worker Harness

A self-feeding worker harness that runs indefinitely. It is a **Spring Boot** application, which is how it demonstrates the recommended way to expose SDK metrics: the SDK publishes into the app's Micrometer registry and Spring Boot Actuator serves the scrape endpoint. The SDK itself starts no metrics web server.

On startup it registers five simulated tasks (`java_worker_0` through `java_worker_4`) and the `java_simulated_tasks_workflow`, then runs two background services:

- **WorkflowGovernor** -- starts a configurable number of `java_simulated_tasks_workflow` instances per second (default 2), indefinitely.
- **SimulatedTaskWorkers** -- five task handlers, each with a codename and a default sleep duration. Each worker supports configurable delay types, failure simulation, and output generation via task input parameters. The workflow chains them in sequence: quickpulse (1s) → whisperlink (2s) → shadowfetch (3s) → ironforge (4s) → deepcrawl (5s).

All resource names use a `java_` prefix so multiple SDK harnesses (C#, Python, Go, etc.) can coexist on the same cluster.

## Metrics

The harness listens on a single HTTP port (`HARNESS_METRICS_PORT`, default `9991`) and serves Actuator endpoints at the root:

- `GET /metrics` -- Prometheus text exposition (the scrape endpoint; Actuator's `prometheus` endpoint remapped to `/metrics`). This matches the cross-SDK PodMonitor contract (port name `metrics` → 9991, path `/metrics`).
- `GET /metrics-json` -- Actuator's JSON metrics browser (the `metrics` endpoint renamed to avoid colliding with the Prometheus path).
- `GET /health` -- Actuator health.

Both SDK metrics (e.g. `task_poll_total`) and standard JVM/process metrics are exposed on the same registry. See `src/main/resources/application.yml`.

## Local Run

```bash
# From the repository root
CONDUCTOR_SERVER_URL=https://your-cluster.example.com/api \
CONDUCTOR_AUTH_KEY=your-key \
CONDUCTOR_AUTH_SECRET=your-secret \
./gradlew :harness:bootRun
```

## Docker

### Build

From the repository root:

```bash
docker build -f harness/Dockerfile -t java-sdk-harness .
```

### Run

```bash
docker run -d \
  -e CONDUCTOR_SERVER_URL=https://your-cluster.example.com/api \
  -e CONDUCTOR_AUTH_KEY=$CONDUCTOR_AUTH_KEY \
  -e CONDUCTOR_AUTH_SECRET=$CONDUCTOR_AUTH_SECRET \
  -e HARNESS_WORKFLOWS_PER_SEC=4 \
  java-sdk-harness
```

### Multiplatform Build and Push

Build for both `linux/amd64` and `linux/arm64` and push to GHCR:

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f harness/Dockerfile \
  -t ghcr.io/conductor-oss/java-sdk/harness-worker:latest \
  --push .
```

After pushing a new image with the same tag, restart the K8s deployment to pull it:

```bash
kubectl rollout restart deployment/java-sdk-harness-worker -n $NS
kubectl rollout status deployment/java-sdk-harness-worker -n $NS
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_SERVER_URL` | yes | -- | Conductor API base URL |
| `CONDUCTOR_AUTH_KEY` | no | -- | Orkes auth key |
| `CONDUCTOR_AUTH_SECRET` | no | -- | Orkes auth secret |
| `HARNESS_WORKFLOWS_PER_SEC` | no | 2 | Workflows to start per second |
| `HARNESS_BATCH_SIZE` | no | 20 | Thread count per worker (controls polling concurrency) |
| `HARNESS_POLL_INTERVAL_MS` | no | 100 | Milliseconds between poll cycles |
| `HARNESS_METRICS_PORT` | no | 9991 | HTTP port for the Actuator/metrics endpoints |
| `HARNESS_PROBE_RATE_PER_SEC` | no | 0 | Control-plane probe rate; 0 disables the probe |
| `HARNESS_WORKERS_ONLY` | no | false | Run workers only (skip registration, governor, and probe) |

## Kubernetes

See [manifests/README.md](manifests/README.md) for deployment instructions.
