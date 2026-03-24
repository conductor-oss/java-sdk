# Multi-Cluster Processing in Java Using Conductor : Partition Data, Process in Parallel Across Clusters, Aggregate

## One Cluster Is Not Enough

Your dataset has grown beyond what a single cluster can process in the required time window. You need to split the workload across clusters in different regions. sending partition A to us-east-1 and partition B to us-west-2, then merge the results. This cuts processing time in half, adds geographic redundancy, and keeps data closer to regional users.

Coordinating multi-cluster processing means partitioning the data correctly, dispatching each partition to the right cluster, waiting for both to finish (while handling the case where one cluster is slower or fails), and merging results that may have different schemas or record counts. Building this with scripts that SSH into different clusters becomes unmanageable.

## The Solution

**You write the per-cluster processing logic. Conductor handles parallel dispatch, cross-region retries, and result aggregation.**

`MclPrepareWorker` takes the dataset size, partitions it into two halves, and returns the partition references. A `FORK_JOIN` dispatches both partitions simultaneously. `MclClusterEastWorker` processes partition A on us-east-1 while `MclClusterWestWorker` processes partition B on us-west-2. The `JOIN` waits for both clusters to complete. `MclAggregateWorker` merges the per-cluster results and record counts into a single output. Conductor handles the parallel dispatch, retries if one cluster's processing fails, and records per-cluster metrics so you can compare processing times and identify regional bottlenecks.

### What You Write: Workers

Four workers handle the multi-cluster pipeline. Data partitioning, parallel per-region processing on us-east-1 and us-west-2, and cross-cluster result aggregation.

### The Workflow

```
mcl_prepare
 │
 ▼
FORK_JOIN
 ├── mcl_cluster_east
 └── mcl_cluster_west
 │
 ▼
JOIN (wait for all branches)
mcl_aggregate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
