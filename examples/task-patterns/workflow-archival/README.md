# Workflow Archival in Java with Conductor

Archival demo workflow. single task for demonstrating cleanup policies.

## The Problem

You need to manage the lifecycle of completed workflow executions. Archiving old runs to keep the active database lean and queries fast. This demo shows a simple batch-processing workflow whose completed executions can be archived using Conductor's archival APIs and cleanup policies.

Without archival, completed workflow data accumulates indefinitely, slowing down queries and consuming storage. Conductor's archival lets you move completed executions to long-term storage while keeping the active dataset manageable.

## The Solution

**You just write the batch processing worker. Conductor handles execution history storage, archival to long-term storage, and cleanup of old runs.**

This example runs a simple batch-processing workflow and then demonstrates Conductor's archival APIs for managing completed execution data. ArchivalTaskWorker takes a `batch` identifier and returns `{ done: true }`. It is intentionally minimal because the focus is on what happens after execution. The example code shows how to run multiple workflow instances, then use Conductor's archival and removal APIs to move completed executions to long-term storage or purge them entirely. This keeps the active execution database lean so that status queries, search, and the Conductor UI remain fast even after millions of workflow runs.

### What You Write: Workers

One minimal worker demonstrates the archival lifecycle: ArchivalTaskWorker processes a batch and returns `{ done: true }`, keeping the focus on how Conductor's archival APIs manage completed execution data rather than on the processing logic itself.

| Worker | Task | What It Does |
|---|---|---|
| **ArchivalTaskWorker** | `arch_task` | Worker for the archival demo workflow. Takes a batch identifier and returns done: true. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
arch_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
