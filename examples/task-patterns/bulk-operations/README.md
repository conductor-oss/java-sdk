# Bulk Operations in Java with Conductor

Bulk operations demo. two-step workflow used for bulk start, pause, resume, and terminate. ## The Problem

You need to manage hundreds or thousands of workflow instances at once. starting a batch of data processing jobs, pausing them while a dependent system is down, resuming them when it recovers, or terminating stale runs. Each batch is identified by a batchId, and each instance runs a two-step pipeline (step1 produces intermediate data, step2 produces the final result). Operating on workflows one at a time through the UI is impractical at scale.

Without Conductor's bulk operations API, you'd build custom scripts that loop through workflow IDs, call start/pause/resume/terminate individually, handle partial failures when some calls succeed and others don't, and track which instances are in which state. That code is fragile, hard to test, and impossible to observe across thousands of concurrent instances.

## The Solution

**You just write the step workers. Conductor handles bulk lifecycle management across all instances.**

This example demonstrates Conductor's bulk operations API for managing workflow instances at scale. The simple two-step workflow serves as the target for bulk start (launching many instances at once), bulk pause (suspending in-flight workflows), bulk resume (continuing paused workflows), and bulk terminate (canceling running workflows). Conductor tracks each instance's state independently, so a partial failure in one batch doesn't affect the others.

### What You Write: Workers

Two step workers form a minimal pipeline that serves as the target for Conductor's bulk lifecycle APIs. Start, pause, resume, and terminate across hundreds of instances at once.

| Worker | Task | What It Does |
|---|---|---|
| **Step1Worker** | `bulk_step1` | First step in the bulk operations workflow. Takes a batchId and returns intermediate data for the batch. |
| **Step2Worker** | `bulk_step2` | Second step in the bulk operations workflow. Takes intermediate data from Step1 and produces the final result. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
bulk_step1
 │
 ▼
bulk_step2

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
