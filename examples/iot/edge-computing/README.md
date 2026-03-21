# Edge Computing Pipeline in Java with Conductor : Task Offloading, On-Device Inference, and Cloud Sync

## Why Edge Compute Pipelines Need Orchestration

Edge computing pushes inference and data processing out to devices at the network edge. cameras running object detection, gateways classifying sensor readings, robots making real-time decisions. But the results still need to flow back to the cloud for aggregation, dashboarding, and long-term storage. That creates a multi-hop pipeline: schedule the compute job on the right edge node, run the inference, sync the results over a potentially unreliable link, and aggregate everything centrally.

Each hop has its own failure mode. The edge node might be offline. The inference job might time out. The sync might fail due to bandwidth constraints or intermittent connectivity. Without orchestration, you'd build a custom pipeline manager that tracks job state across edge and cloud, retries failed syncs with exponential backoff, and logs everything manually. That manager becomes a distributed systems problem in itself.

## How This Workflow Solves It

**You just write the edge pipeline workers. Task offloading, on-device inference, result syncing, and cloud aggregation. Conductor handles multi-hop sequencing, unreliable-link sync retries, and latency tracking at every hop for pipeline optimization.**

Each stage of the edge-to-cloud pipeline is an independent worker. offload the job, run the inference, sync the results, aggregate in the cloud. Conductor sequences them, passes inference outputs (detected objects, classification labels, confidence scores) through to the sync and aggregation stages, retries failed syncs automatically, and tracks latency and data sizes at every hop. You get a reliable edge compute pipeline without writing state management or retry logic.

### What You Write: Workers

Four workers span the edge-to-cloud pipeline: OffloadTaskWorker schedules compute jobs on edge nodes, ProcessEdgeWorker runs on-device ML inference, SyncResultsWorker uploads results to cloud storage, and AggregateCloudWorker feeds dashboards and time-series stores.

| Worker | Task | What It Does |
|---|---|---|
| **AggregateCloudWorker** | `edg_aggregate_cloud` | Aggregates synced edge results into cloud dashboards and time-series stores. |
| **OffloadTaskWorker** | `edg_offload_task` | Schedules a compute job on the target edge node and returns the edge job ID. |
| **ProcessEdgeWorker** | `edg_process_edge` | Runs on-device ML inference (object detection, classification) and returns results with processing time. |
| **SyncResultsWorker** | `edg_sync_results` | Uploads inference results from the edge node to cloud storage, tracking sync latency and payload size. |

the workflow and alerting logic stay the same.

### The Workflow

```
edg_offload_task
 │
 ▼
edg_process_edge
 │
 ▼
edg_sync_results
 │
 ▼
edg_aggregate_cloud

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
