# Multi Cluster

A platform operates Conductor clusters in multiple availability zones. Work must be distributed across clusters, failed clusters must be detected, tasks must be re-routed to healthy clusters, and the system must rebalance when a recovered cluster rejoins.

## Pipeline

```
[mcl_prepare]
     |
     v
     +─────────────────────────────────────────+
     | [mcl_cluster_east] | [mcl_cluster_west] |
     +─────────────────────────────────────────+
     [join]
     |
     v
[mcl_aggregate]
```

**Workflow inputs:** `jobId`, `datasetSize`

## Workers

**MclAggregateWorker** (task: `mcl_aggregate`)

- Reads `eastCount`, `westCount`. Writes `totalProcessed`, `clusters`

**MclClusterEastWorker** (task: `mcl_cluster_east`)

- Writes `processed`, `recordCount`, `latencyMs`

**MclClusterWestWorker** (task: `mcl_cluster_west`)

- Writes `processed`, `recordCount`, `latencyMs`

**MclPrepareWorker** (task: `mcl_prepare`)

- Reads `datasetSize`. Writes `partitionA`, `partitionB`

---

**16 tests** | Workflow: `multi_cluster_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
