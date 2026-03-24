# On Call Rotation in Java with Conductor

Automates on-call rotation handoffs using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks the rotation schedule, hands off active incidents between engineers, updates PagerDuty/alerting routing rules, and confirms the new on-call has acknowledged.

## Seamless On-Call Handoffs

It is Monday morning and on-call shifts from Alice to Bob. Two incidents are still active. The handoff needs to happen cleanly: Alice briefs Bob on open issues, PagerDuty routing updates so new alerts go to Bob, and Bob confirms he is ready. If any step is missed, alerts fall into a black hole or wake up the wrong person at 3 AM.

Without orchestration, you'd rely on a shared Google Calendar and a Slack message saying "you're on-call now." PagerDuty routing stays pointed at Alice until someone remembers to update it. Active incidents don't get handed off. Bob discovers them when a customer escalates. If the routing update fails silently, alerts go to the wrong person for hours. There's no record of whether the handoff actually completed, who acknowledged, or which incidents were transferred.

## The Solution

**You write the schedule lookup and routing update logic. Conductor handles handoff sequencing, confirmation gates, and rotation audit trails.**

Each stage of the on-call handoff is a simple, independent worker. The schedule checker looks up the rotation to determine who is currently on-call and who is next. Supporting weekly, daily, or custom rotation types per team. The handoff worker transfers active incidents from the outgoing to the incoming engineer, summarizing open issues, severity levels, and any context needed to pick up where the previous on-call left off. The routing updater changes PagerDuty escalation policies and alerting rules so new alerts reach the right person immediately. The confirmation worker verifies the incoming on-call has acknowledged the handoff and is ready to respond. Conductor executes them in strict sequence, ensures routing only updates after incidents are transferred, retries if PagerDuty's API is temporarily unavailable, and tracks every handoff so you can audit who was on-call when.

### What You Write: Workers

Four workers manage the handoff. Checking the schedule, transferring incident context, updating alerting routes, and confirming acknowledgment.

| Worker | Task | What It Does |
|---|---|---|
| **CheckScheduleWorker** | `oc_check_schedule` | Checks the rotation schedule to determine who is going off-call and who is coming on (e.g., Alice to Bob) |
| **ConfirmWorker** | `oc_confirm` | Verifies the new on-call engineer has acknowledged and is ready to receive alerts |
| **HandoffWorker** | `oc_handoff` | Transfers active incident context from the outgoing to the incoming on-call engineer |
| **UpdateRoutingWorker** | `oc_update_routing` | Updates PagerDuty/Opsgenie routing rules to direct new alerts to the incoming on-call |

the workflow and rollback logic stay the same.

### The Workflow

```
oc_check_schedule
 │
 ▼
oc_handoff
 │
 ▼
oc_update_routing
 │
 ▼
oc_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
