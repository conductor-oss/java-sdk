# Incident Response Automation in Java with Conductor

The PagerDuty alert fired at 2:14 AM. The on-call engineer saw the Slack notification, opened their laptop, SSHed into the wrong box, ran `top` for a while, then remembered they needed to check the dashboard, which was on a different VPN. Forty minutes later they found the actual issue: the API gateway was at 95% CPU. They scaled it up manually, forgot to create an incident ticket, and went back to sleep. The status page still says "All Systems Operational." Tomorrow, nobody will know what happened, what was tried, or whether the fix actually worked. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate incident response end-to-end: create the ticket, page the responder, pull diagnostics, and attempt automated remediation, with a complete audit trail of every step.

## When Incidents Strike

A production alert fires at 2 AM. Someone needs to create an incident ticket, page the on-call engineer, pull CPU and error-rate diagnostics from the affected service, and attempt an automated fix (like scaling up replicas), all before the SLA window closes. If any step fails silently or runs out of order, mean-time-to-recovery climbs and customers notice.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the incident handling logic. Conductor handles step sequencing, retries, and the complete incident audit trail.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle the incident lifecycle. Creating the ticket, paging the responder, pulling diagnostics, and attempting automated remediation.

| Worker | Task | What It Does |
|---|---|---|
| `CreateIncidentWorker` | `ir_create_incident` | Creates a tracked incident record with ID `INC-42` and the provided severity level (e.g., P1) |
| `NotifyOncallWorker` | `ir_notify_oncall` | Pages the current on-call engineer with the incident ID so they can begin investigation |
| `GatherDiagnosticsWorker` | `ir_gather_diagnostics` | Collects live metrics from the affected service. Returns CPU usage (95%) and error rate (5%) |
| `AutoRemediateWorker` | `ir_auto_remediate` | Attempts automated recovery (scales up 2 replicas) and reports whether remediation succeeded |

### The Workflow

```
ir_create_incident
 |
 v
ir_notify_oncall
 |
 v
ir_gather_diagnostics
 |
 v
ir_auto_remediate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
