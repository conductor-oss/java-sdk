# Distributed Logging in Java Using Conductor : Multi-Service Log Collection and Correlation

## The Problem

A distributed transaction spans three services, and you need to see what happened across all of them for a given request. Logs must be collected from each service (different log formats, different storage locations), then correlated by trace ID into a single chronological view. Without correlation, debugging a failed request means manually searching logs in three different systems and mentally stitching them together.

Without orchestration, log correlation is a manual process using grep across multiple log stores. Collection from different services runs serially instead of in parallel, slowing down incident response. When one service's log store is temporarily unavailable, the entire investigation stalls.

## The Solution

**You just write the log collection queries and trace correlation logic. Conductor handles parallel log collection across services, retries when individual log stores are slow, and timing data showing how long each service's collection took.**

Conductor's FORK/JOIN collects logs from all three services in parallel. if one service's log store is slow, the others don't wait. A correlation worker then stitches the logs together by trace ID into a unified timeline. Every collection and correlation run is tracked with timing, you can see which services responded and how long each took.

### What You Write: Workers

Three per-service collectors (CollectSvc1Worker, CollectSvc2Worker, CollectSvc3Worker) run in parallel via FORK/JOIN to gather logs from each microservice, then a CorrelateWorker stitches them into a unified timeline by trace ID.

| Worker | Task | What It Does |
|---|---|---|
| **CollectSvc1Worker** | `dg_collect_svc1` | Collects logs from service 1 for a given trace ID, returning the log count |
| **CollectSvc2Worker** | `dg_collect_svc2` | Collects logs from service 2 for a given trace ID, returning the log count |
| **CollectSvc3Worker** | `dg_collect_svc3` | Collects logs from service 3 for a given trace ID, returning the log count |
| **CorrelateWorker** | `dg_correlate` | Correlates logs from all three services by trace ID into a unified timeline, identifying the request flow across services |

the schedule triggers, retry behavior, and monitoring stay the same.

### The Workflow

```
FORK_JOIN
 ├── dg_collect_svc1
 ├── dg_collect_svc2
 └── dg_collect_svc3
 │
 ▼
JOIN (wait for all branches)
dg_correlate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
