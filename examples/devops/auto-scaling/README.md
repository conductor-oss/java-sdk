# Auto-Scaling in Java with Conductor : Analyze Metrics, Plan Scaling, Execute, Verify

Analyzes service metrics, plans scaling action, executes scaling, and verifies the result. Pattern: analyze -> plan -> execute -> verify.

## Scaling Decisions Need Analysis, Not Just Thresholds

CPU is at 80%. Should you scale up? Maybe, or maybe it's a transient spike from a batch job that ends in 5 minutes. Auto-scaling needs more than simple threshold crossing: analyze the metric trend (is it sustained or transient?), plan the scaling (how many instances, which instance type?), execute the scaling operation, and verify the new instances are healthy and handling traffic.

Scaling too aggressively wastes money (spinning up 10 instances for a 2-minute spike). Scaling too conservatively risks outages (waiting too long while latency degrades). The analyze step should consider metric history, time of day, and business context (is this a known traffic pattern?). The verify step must confirm new instances are healthy before declaring success.

## The Solution

**You write the metric analysis and scaling logic. Conductor handles the analyze-plan-execute-verify sequence and records every scaling decision.**

`AnalyzeWorker` examines current metrics (CPU utilization, memory pressure, request rate, queue depth) and their trends over the analysis window to determine if scaling is needed. `PlanWorker` determines the scaling action. scale up (add instances), scale down (remove instances), or maintain, with the target instance count based on the metric analysis. `ExecuteWorker` performs the scaling operation, launching new instances or terminating excess ones. `VerifyWorker` confirms the scaled service is healthy, new instances passing health checks, traffic being distributed, and metrics improving. Conductor records every scaling decision with its metric context for capacity planning analysis.

### What You Write: Workers

Four workers handle the scaling decision. Analyzing current metrics, planning the scaling action, executing instance changes, and verifying the result.

| Worker | Task | What It Does |
|---|---|---|
| **Analyze** | `as_analyze` | Analyzes current service metrics (CPU, memory, request rate) to determine load. |
| **Execute** | `as_execute` | Executes the planned scaling action (scale-up, scale-down, or no-change). |
| **Plan** | `as_plan` | Plans the scaling action based on current load analysis. |
| **Verify** | `as_verify` | Verifies that the scaling action achieved the desired result. |

the workflow and rollback logic stay the same.

### The Workflow

```
as_analyze
 │
 ▼
as_plan
 │
 ▼
as_execute
 │
 ▼
as_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
