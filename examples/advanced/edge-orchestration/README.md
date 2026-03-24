# Edge Orchestration

An IoT deployment processes sensor data at the network edge before forwarding summaries to the cloud. Edge workers have limited CPU and memory. The orchestration pipeline needs to classify data locally, filter noise at the edge, aggregate summaries, and sync results to the cloud on a schedule.

## Pipeline

```
[eor_dispatch]
     |
     v
[eor_edge_process]
     |
     v
[eor_collect]
     |
     v
[eor_merge]
```

**Workflow inputs:** `jobId`, `edgeNodes`

## Workers

**EorCollectWorker** (task: `eor_collect`)

- Writes `collected`, `nodeCount`, `totalRecords`

**EorDispatchWorker** (task: `eor_dispatch`)

- Writes `dispatched`, `assignments`

**EorEdgeProcessWorker** (task: `eor_edge_process`)

- Writes `results`

**EorMergeWorker** (task: `eor_merge`)

- Writes `mergedResult`, `complete`

---

**16 tests** | Workflow: `edge_orchestration_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
