# Distributed Locking in Java with Conductor

Distributed locking for concurrency control. ## The Problem

When multiple service instances process the same resource concurrently, you need a distributed lock to prevent race conditions. The workflow acquires a lock with a TTL on a named resource, executes the critical-section operation while holding the lock, and then releases it. If the process crashes, the TTL ensures the lock is eventually released.

Without orchestration, distributed locking is implemented inline with try/finally blocks around Redis or ZooKeeper calls. If the process crashes between acquiring and releasing, the lock may be held until TTL expires with no visibility into what operation was in progress.

## The Solution

**You just write the lock-acquire, critical-section, and lock-release workers. Conductor handles ordered lock lifecycle, crash-safe state so locks are eventually freed, and full audit of lock acquisitions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Three workers enforce mutual exclusion: AcquireLockWorker obtains a distributed lock with a TTL, ExecuteCriticalWorker performs the protected operation, and ReleaseLockWorker frees the lock token.

| Worker | Task | What It Does |
|---|---|---|
| **AcquireLockWorker** | `dl_acquire_lock` | Acquires a distributed lock on the specified resource with a TTL and returns a lock token. |
| **ExecuteCriticalWorker** | `dl_execute_critical` | Executes the critical-section operation on the locked resource (e.g., update a shared counter). |
| **ReleaseLockWorker** | `dl_release_lock` | Releases the distributed lock using the lock token. |

the workflow coordination stays the same.

### The Workflow

```
dl_acquire_lock
 │
 ▼
dl_execute_critical
 │
 ▼
dl_release_lock

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
