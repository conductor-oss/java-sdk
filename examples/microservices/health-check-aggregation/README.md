# Aggregating Health from API, Database, Cache, and Queue in Parallel

Your load balancer's `/health` endpoint returns "ok" but does not tell you that Kafka has
a 12-message lag or Postgres is at 23/100 connections. This workflow checks all four
infrastructure components in parallel using FORK_JOIN and aggregates them into a single
HEALTHY or DEGRADED status.

## Workflow

```
(no inputs)
    |
    v
  FORK_JOIN ---------------------------------+
  |            |            |                |
  v            v            v                v
+-----------+ +-----------+ +-----------+ +-----------+
| hc_check_ | | hc_check_ | | hc_check_ | | hc_check_ |
| api       | | db        | | cache     | | queue     |
+-----------+ +-----------+ +-----------+ +-----------+
  api-gateway   postgres      redis         kafka
  15ms latency  23/100 conn   45% memory    12 msg lag
  |            |            |                |
  +------ JOIN +------------+----------------+
               |
               v
    +-----------------------+
    | hc_aggregate_health   |   status: "healthy" or "degraded"
    +-----------------------+
```

## Workers

**CheckApiWorker** -- API gateway: `healthy: true`, `latencyMs: 15`, `component: "api-gateway"`.

**CheckDbWorker** -- Postgres: `healthy: true`, `connections: 23`, `component: "postgres"`.

**CheckCacheWorker** -- Redis: `healthy: true`, `memoryPct: 45`, `component: "redis"`.

**CheckQueueWorker** -- Kafka: `healthy: true`, `lag: 12`, `component: "kafka"`.

**AggregateHealthWorker** -- Combines all results. Returns `status: "healthy"` if all
components are healthy, otherwise `"degraded"`.

## Tests

10 unit tests cover each component check and the aggregation logic.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
