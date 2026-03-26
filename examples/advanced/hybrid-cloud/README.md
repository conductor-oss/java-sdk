# Hybrid Cloud

An enterprise runs sensitive workloads on-premise and burst workloads in the public cloud. The hybrid pipeline needs to classify each task by data sensitivity, route sensitive tasks to on-premise workers and burst tasks to cloud workers, and aggregate results regardless of where they executed.

## Pipeline

```
[hyb_classify_data]
     |
     v
     <SWITCH>
       |-- onprem -> [hyb_process_onprem]
       |-- cloud -> [hyb_process_cloud]
```

**Workflow inputs:** `dataId`, `dataType`, `payload`

## Workers

**HybClassifyDataWorker** (task: `hyb_classify_data`)

Classifies incoming data by sensitivity. Checks `dataType` against a hardcoded sensitive-type list containing `"pii"`, `"phi"`, and `"financial"`. If matched, sets `classification` = `"confidential"` and `target` = `"onprem"`. Otherwise sets `classification` = `"public"` and `target` = `"cloud"`. The `target` output drives the downstream SWITCH task.

- Reads `dataType`. Writes `classification`, `target`

**HybProcessOnpremWorker** (task: `hyb_process_onprem`)

Processes confidential data on the organization's own infrastructure. Reports `processedAt` = `"datacenter-1"` and `encrypted` = true, keeping sensitive data within the physical security boundary.

- Writes `processedAt`, `encrypted`

**HybProcessCloudWorker** (task: `hyb_process_cloud`)

Processes non-sensitive data in the public cloud for elastic scaling. Reports `processedAt` = `"aws-us-east-1"` and `scaledInstances` = 4, indicating horizontal scaling was applied.

- Writes `processedAt`, `scaledInstances`

---

**12 tests** | Workflow: `hybrid_cloud_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
