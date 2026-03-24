# Implementing Self-Healing Workflow in Java with Conductor : Health Check, Diagnosis, Remediation, and Retry

## The Problem

Your service goes unhealthy. high error rate, degraded performance, resource exhaustion. Instead of paging an engineer at 3 AM, you want the system to heal itself: detect the issue via health check, diagnose the root cause (memory leak, disk full, dependency down), apply the appropriate fix (restart the service, clear the cache, scale up replicas), and retry the original operation. Self-healing reduces mean time to recovery from minutes (human response) to seconds (automated).

Without orchestration, self-healing logic is scattered across monitoring scripts, cron jobs, and runbooks. Health checks live in Nagios, remediation scripts live on jump boxes, and the retry logic is manual. Building a coherent self-healing loop that diagnoses before remediating (don't restart if the issue is a full disk) requires gluing together multiple tools.

## The Solution

**You just write the health check and remediation actions. Conductor handles SWITCH-based routing between healthy and unhealthy paths, the check-diagnose-remediate-retry sequence, retries on remediation steps, and a complete record of every healing attempt showing what was detected, what fix was applied, and whether recovery succeeded.**

Each self-healing step is an independent worker. health check evaluates the service, diagnose identifies the root cause, remediate applies the fix, and retry re-runs the original operation. Conductor orchestrates the flow: when the health check fails, it routes to diagnosis and remediation before retrying. Every healing attempt is tracked, you can see what was detected, what fix was applied, and whether the retry succeeded.

### What You Write: Workers

HealthCheckWorker evaluates service status, DiagnoseWorker identifies the root cause when unhealthy, RemediateWorker applies the appropriate fix (restart, cache clear, scale up), and RetryProcessWorker re-runs the original operation after recovery.

| Worker | Task | What It Does |
|---|---|---|
| **DiagnoseWorker** | `sh_diagnose` | Worker for sh_diagnose. diagnoses problems in an unhealthy service. Analyzes symptoms and returns a diagnosis with r.. |
| **HealthCheckWorker** | `sh_health_check` | Worker for sh_health_check. checks the health of a service. If service is "broken-service", returns healthy="false" .. |
| **ProcessWorker** | `sh_process` | Worker for sh_process. normal processing for healthy services. Returns result="processed-{data}" where data comes fr.. |
| **RemediateWorker** | `sh_remediate` | Worker for sh_remediate. applies the fix recommended by diagnosis. Takes the diagnosis and action, applies the remed.. |
| **RetryProcessWorker** | `sh_retry_process` | Worker for sh_retry_process. retries processing after remediation. Returns result="healed-{data}" to indicate the se.. |

Workers implement success and failure scenarios so you can observe the resilience pattern end-to-end. Swap in real service calls and the retry, compensation, and recovery behavior works identically.

### The Workflow

```
sh_health_check
 │
 ▼
SWITCH (health_switch_ref)
 ├── false: sh_diagnose -> sh_remediate -> sh_retry_process
 └── default: sh_process

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
