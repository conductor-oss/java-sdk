# Bug Triage in Java with Conductor : Severity-Based Routing for Incoming Bug Reports

## Routing Bugs by Severity Automatically

When a bug report comes in, someone has to read it, decide how severe it is, and route it accordingly. Critical bugs (crashes, data loss) need immediate attention and different handling than cosmetic issues. Doing this manually is slow and inconsistent. severity classification depends on who reads the report and when.

This workflow automates the entire triage process. It parses the raw bug report into structured fields (title, description, component), classifies severity by analyzing the description for keywords, then uses a Conductor SWITCH task to route to the appropriate handler: `btg_handle_critical` for crashes and data loss, `btg_handle_high` for errors and broken features, or `btg_handle_low` for everything else. After the severity-specific handling completes, the bug is assigned to a developer based on severity and component.

## The Solution

**You just write the bug-parsing, severity-classification, and assignment workers. Conductor handles the severity-based routing and pipeline sequencing.**

Six workers handle the triage pipeline. report parsing, severity classification, three severity-specific handlers, and developer assignment. The SWITCH task makes the routing logic declarative: Conductor reads the `severity` output from the classifier and routes to the matching branch automatically. The critical-path handler can page on-call engineers while the low-priority handler simply adds to the backlog, each handler is independent and swappable.

### What You Write: Workers

Six workers handle the triage pipeline. ParseReportWorker extracts structured fields, ClassifySeverityWorker assigns a severity level, three handlers process critical/high/low bugs differently, and AssignWorker routes to the right developer.

| Worker | Task | What It Does |
|---|---|---|
| **AssignWorker** | `btg_assign` | Assigns the triaged bug to a developer based on severity and component. |
| **ClassifySeverityWorker** | `btg_classify_severity` | Scans the bug description for keywords (crash, data loss, error) to classify severity as critical, high, or low. |
| **HandleCriticalWorker** | `btg_handle_critical` | Escalates the bug by paging the on-call engineer for immediate response. |
| **HandleHighWorker** | `btg_handle_high` | Flags the bug for inclusion in the next sprint. |
| **HandleLowWorker** | `btg_handle_low` | Adds the bug to the backlog for future prioritization. |
| **ParseReportWorker** | `btg_parse_report` | Parses the raw bug report text into structured fields: title, description, and component. |

### The Workflow

```
btg_parse_report
 │
 ▼
btg_classify_severity
 │
 ▼
SWITCH (btg_switch_ref)
 ├── critical: btg_handle_critical
 ├── high: btg_handle_high
 └── default: btg_handle_low
 │
 ▼
btg_assign

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
