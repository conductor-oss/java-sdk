# Java SDK Worker Harness

A self-feeding worker harness that runs indefinitely. On startup it registers five simulated tasks (`java_worker_0` through `java_worker_4`) and the `java_simulated_tasks_workflow`, then runs two background services:

- **WorkflowGovernor** -- starts a configurable number of `java_simulated_tasks_workflow` instances per second (default 2), indefinitely.
- **SimulatedTaskWorkers** -- five task handlers, each with a codename and a default sleep duration. Each worker supports configurable delay types, failure simulation, and output generation via task input parameters. The workflow chains them in sequence: quickpulse (1s) → whisperlink (2s) → shadowfetch (3s) → ironforge (4s) → deepcrawl (5s).

All resource names use a `java_` prefix so multiple SDK harnesses (C#, Python, Go, etc.) can coexist on the same cluster.

## Local Run

```bash
# From the repository root
CONDUCTOR_SERVER_URL=https://your-cluster.example.com/api \
CONDUCTOR_AUTH_KEY=your-key \
CONDUCTOR_AUTH_SECRET=your-secret \
./gradlew :harness:run
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
| `HARNESS_ADAPTIVE` | no | false | Use adaptive concurrency instead of `HARNESS_BATCH_SIZE` threads per worker |
| `HARNESS_ADAPTIVE_MAX_PROCESS_CONCURRENCY` | no | 256 | Adaptive concurrency ceiling for the process |
| `HARNESS_ADAPTIVE_INITIAL_PROCESS_CONCURRENCY` | no | 0 | Adaptive starting target; 0 uses min(task types, processors) |
| `HARNESS_ADAPTIVE_MIN_PROCESS_CONCURRENCY` | no | 1 | Target an idle process decays to, so bursts do not start from one slot |
| `HARNESS_ADAPTIVE_STANDBY_POLL_TIMEOUT_MS` | no | 1000 | Server wait of the long poll each queue keeps open on spare capacity; 0 disables it |
| `HARNESS_ADAPTIVE_MAX_WORKER_CONCURRENCY` | no | 64 | Adaptive concurrency ceiling per worker |
| `HARNESS_VIRTUAL_THREADS` | no | false | Run tasks on virtual threads |

## Kubernetes

See [manifests/README.md](manifests/README.md) for deployment instructions.
