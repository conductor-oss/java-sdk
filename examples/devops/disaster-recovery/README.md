# Disaster Recovery in Java with Conductor

Orchestrates a full disaster recovery failover using [Conductor](https://github.com/conductor-oss/conductor). When the primary region goes down, this workflow detects the failure, promotes the standby database in the DR region, updates DNS to point traffic to the backup, and verifies the recovery. Tracking RTO throughout.

## When the Primary Region Goes Down

Your primary region (us-east-1) suffers an outage. The database needs to be failed over to the standby in us-west-2, DNS records must be updated to redirect traffic, and someone needs to verify the DR region is healthy, all within your RTO target. Doing these steps manually under pressure risks mistakes, missed steps, and blown SLAs.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the failover logic. Conductor handles step ordering, RTO tracking, and guaranteed recovery completion.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the DR failover sequence. Detecting the outage, promoting the standby database, updating DNS, and verifying recovery within RTO.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `dr_detect` | Confirms the primary region failure by checking health endpoints and marks the outage as verified |
| **FailoverDbWorker** | `dr_failover_db` | Promotes the standby database replica in the DR region to primary |
| **UpdateDnsWorker** | `dr_update_dns` | Updates DNS records to redirect traffic from the failed primary to the DR region |
| **VerifyWorker** | `dr_verify` | Validates the DR region is healthy and serving traffic, and reports the achieved RTO in minutes |

the workflow and rollback logic stay the same.

### The Workflow

```
dr_detect
 │
 ▼
dr_failover_db
 │
 ▼
dr_update_dns
 │
 ▼
dr_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
