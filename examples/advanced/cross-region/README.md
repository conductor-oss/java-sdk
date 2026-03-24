# Cross-Region Data Replication in Java Using Conductor : Replicate, Sync, Verify Consistency

## Keeping Data Consistent Across Regions

Multi-region architectures improve latency and availability, but replicating data from us-east-1 to eu-west-1 is not a simple copy. You need to initiate the replication, wait for the data to sync, compute checksums on both sides, and verify they match. If the checksums diverge. because a write landed on the primary during sync, or a network partition caused partial replication, you need to know immediately, not discover it hours later when a user in Europe sees stale data.

Manually coordinating replication means writing polling loops to check sync status, implementing checksum verification across regions with different API endpoints, handling transient failures on cross-region network calls, and logging enough context to diagnose consistency drift. Each step depends on the previous one. you can't verify consistency before sync completes, and sync requires a valid replication ID from the initial copy.

## The Solution

**You write the replication and consistency checks. Conductor handles sequencing, retries, and cross-region audit trails.**

`XrReplicateWorker` initiates the data replication from the primary region to the replica region and returns a replication ID. `XrSyncWorker` waits for synchronization to complete and computes checksums for both the primary and replica copies. `XrVerifyConsistencyWorker` compares the checksums. if they match, the regions are consistent; if they diverge, the workflow output flags the inconsistency. Conductor sequences these steps, retries any that fail due to cross-region network issues, and records the replication ID, checksums, and consistency verdict for every run.

### What You Write: Workers

Three workers own the replication lifecycle: data copying between regions, checksum synchronization, and consistency verification, each targeting one phase of cross-region durability.

### The Workflow

```
xr_replicate
 │
 ▼
xr_sync
 │
 ▼
xr_verify_consistency

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
