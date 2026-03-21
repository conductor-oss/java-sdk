# Uptime Monitoring in Java with Conductor: Endpoint Health Checks, Alerting, and Escalation

## The Problem

You need to monitor the health of multiple endpoints across your infrastructure. Each check involves different concerns. DNS resolution, HTTP availability, TLS certificate validity. When something goes wrong, the right people need to be notified through the right channels (Slack, email, status page), and if failures persist, escalation kicks in (SMS, PagerDuty).

Without orchestration, you'd wire all of this together in a single monolithic script. Managing threads for parallelism, writing if/else chains for routing, building retry loops with backoff, adding try/catch everywhere for failure handling, and bolting on logging to understand what happened. That code becomes brittle, hard to change, and impossible to observe at scale.

## The Solution

**You write the endpoint checks and notification logic. Conductor handles parallel health checking, severity-based routing, escalation policies, and full execution history.**

Each concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of running them in parallel, routing based on results, retrying on failure, tracking every execution, and resuming if the process crashes. ### What You Write: Workers

Eight workers cover the monitoring lifecycle, from parallel endpoint checks through result aggregation, multi-channel alerting, and escalation to on-call.

| Worker | What It Does |
|---|---|---|
| **CheckEndpoint** | Performs real HTTP, DNS, and TLS health checks against a single endpoint |
| **AggregateResults** | Combines results from all checks, determines overall system status |
| **SendSlackAlert** | Sends Slack notification via webhook |
| **SendEmailAlert** | Sends email alerts to the ops team |
| **UpdateStatusPage** | Updates the public status page with component statuses |
| **CheckEscalation** | Escalates when failing endpoints meet or exceed threshold |
| **SendSmsAlert** | Sends SMS alerts for critical escalations |
| **PageOncall** | Pages the on-call engineer |
| **RecordHealthy** | Logs healthy status when all endpoints pass |
| **StoreMetrics** | Writes monitoring data points |

Example, the entire CheckEndpoint worker is just a class that makes HTTP/DNS/TLS calls and returns the result:

```java
public class CheckEndpoint implements Worker {
 @Override
 public String getTaskDefName() { return "uptime_check_endpoint"; }

 @Override
 public TaskResult execute(Task task) {
 String url = (String) task.getInputData().get("url");
 // Do the actual HTTP, DNS, TLS checks...
 TaskResult result = new TaskResult(task);
 result.setStatus(TaskResult.Status.COMPLETED);
 result.getOutputData().put("status", "healthy");
 result.getOutputData().put("responseTimeMs", 183);
 return result;
 }
}

```

No retry logic. No error routing. No thread management. Just the business logic.

### The Workflow

The workflow definition (`workflow.json`) is a simple JSON file that describes how the workers connect. No imperative code. Just declare the flow:

```
PrepareChecks
 │
 ▼
FORK_JOIN_DYNAMIC ──► [CheckEndpoint] x N (parallel)
 │
 ▼
AggregateResults
 │
 ▼
SWITCH (hasFailures?)
 │
 ├── true ──► FORK_JOIN ──► [Slack + Email + StatusPage] (parallel)
 │ │
 │ ▼
 │ CheckEscalation
 │ │
 │ ▼
 │ SWITCH (shouldEscalate?)
 │ ├── true ──► FORK_JOIN ──► [SMS + PagerDuty] (parallel)
 │ └── false
 │
 └── false ──► RecordHealthy
 │
 ▼
StoreMetrics

```

## Endpoints Checked

The example checks these real public endpoints:

| Endpoint | Expected | Purpose |
|---|---|---|
| https://www.google.com | 200 | Reliable external endpoint |
| https://github.com | 200 | Developer service |
| https://www.cloudflare.com | 200 | CDN/infrastructure provider |
| https://down.example.invalid | 200 | **Always fails**. `.invalid` TLD guarantees DNS failure, no internet dependency |

Each endpoint gets three real network checks:
- **DNS**: hostname resolution via `InetAddress.getAllByName()`
- **HTTP**: GET request with response time measurement via `HttpURLConnection`
- **TLS**: certificate validation and expiry check via `SSLSocket`

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
