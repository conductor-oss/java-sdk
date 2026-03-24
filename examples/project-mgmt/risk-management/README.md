# Project Risk Management in Java with Conductor : Identification, Severity Assessment, and Mitigation Planning

## The Problem

You need to manage risks across a project portfolio. When a new risk is reported. "key vendor may miss delivery deadline," "database migration could cause downtime", someone has to identify and categorize it, assess severity and probability, route it to the right response process (executive escalation for high-severity, team-level review for medium, backlog tracking for low), and generate a mitigation plan. Each of these steps depends on the output of the previous one, and the routing logic changes based on the assessed severity.

Without orchestration, you'd wire all of this into a single monolithic class. if/else chains to route by severity, try/catch blocks around every step, manual logging to audit which risks were escalated and why. That code becomes brittle as severity categories change, hard to extend when you add new response processes, and impossible to audit when stakeholders ask "why wasn't this risk escalated?"

## The Solution

**You just write the risk identification, severity assessment, severity-based handling, and mitigation planning logic. Conductor handles assessment retries, mitigation routing, and risk tracking audit trails.**

Each concern is a simple, independent worker. a plain Java class that does one thing: identify the risk, assess its severity, handle the high/medium/low response, or build the mitigation plan. Conductor takes care of executing them in the right order, routing to the correct severity handler via a SWITCH task, retrying on failure, tracking every execution, and resuming if the process crashes.

### What You Write: Workers

Risk identification, probability assessment, mitigation planning, and monitoring workers each address one stage of project risk governance.

| Worker | Task | What It Does |
|---|---|---|
| **IdentifyWorker** | `rkm_identify` | Parses the risk description, assigns a risk ID, and categorizes it (technical, schedule, resource, external) |
| **AssessWorker** | `rkm_assess` | Evaluates severity (high/medium/low), probability, and business impact of the identified risk |
| **HighWorker** | `rkm_high` | Triggers executive escalation and immediate response actions for high-severity risks |
| **MediumWorker** | `rkm_medium` | Schedules team-level review and assigns a risk owner for medium-severity risks |
| **LowWorker** | `rkm_low` | Logs the risk to the backlog for periodic review (default path for low-severity risks) |
| **MitigateWorker** | `rkm_mitigate` | Generates a mitigation plan based on the project context and assessed severity |

### The Workflow

```
rkm_identify
 │
 ▼
rkm_assess
 │
 ▼
SWITCH (switch_ref)
 ├── high: rkm_high
 ├── medium: rkm_medium
 └── default: rkm_low
 │
 ▼
rkm_mitigate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
