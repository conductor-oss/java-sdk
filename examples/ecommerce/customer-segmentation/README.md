# Customer Segmentation

Customer segmentation: collect data, cluster, label segments, target

**Input:** `datasetId`, `numSegments` | **Timeout:** 60s

## Pipeline

```
seg_collect_data
    │
seg_cluster
    │
seg_label_segments
    │
seg_target
```

## Workers

**ClusterWorker** (`seg_cluster`)

Reads `numSegments`. Outputs `clusters`.

**CollectDataWorker** (`seg_collect_data`)

Reads `datasetId`. Outputs `customers`, `totalCustomers`.

**LabelSegmentsWorker** (`seg_label_segments`)

Outputs `segments`.

**TargetWorker** (`seg_target`)

Reads `segments`. Outputs `campaigns`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
