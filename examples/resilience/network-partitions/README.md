# Implementing Network Partition Handling in Java with Conductor : Resilient Workers with Reconnection Tracking

## The Problem

In distributed systems, network partitions happen. the worker loses connectivity to the Conductor server due to infrastructure issues, DNS failures, or cloud provider outages. During a partition, the worker can't poll for tasks or report results. When connectivity resumes, the worker must reconnect and resume processing without duplicating work or losing progress.

Without orchestration, network partition handling means custom reconnection logic, heartbeat monitoring, and manual state reconciliation. Each worker implements its own retry-on-disconnect behavior. Some workers crash on disconnect, some silently stop processing, and some lose in-flight work.

## The Solution

The worker tracks connection attempts and handles reconnection state. Conductor's polling model is inherently partition-tolerant. when the network heals, the worker simply resumes polling. Tasks that timed out during the partition are retried automatically. The full history of connection attempts and task executions is preserved. ### What You Write: Workers

NetworkPartitionWorker tracks connection attempts and processes tasks resiliently, automatically resuming work when connectivity to the Conductor server is restored after a network interruption.

| Worker | Task | What It Does |
|---|---|---|
| **NetworkPartitionWorker** | `np_resilient_task` | Worker for np_resilient_task. Demonstrates handling network partitions. Tracks attempt count using an AtomicInteger.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
np_resilient_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
