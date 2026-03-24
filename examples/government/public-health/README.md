# Public Health Surveillance in Java with Conductor : Disease Monitoring, Outbreak Detection, and Response Coordination

## The Problem

You need to run disease surveillance for a public health department. Case reports come in for a specific disease and region. The system must pull baseline epidemiological data, compare the current case count against expected levels to detect whether an outbreak is occurring, and then take the right action. issue a public health alert if cases exceed the threshold, or schedule continued monitoring if levels are elevated but not yet critical. Regardless of the branch taken, a response plan must be executed. The decision to alert versus monitor must be automatic and auditable.

Without orchestration, you'd build a monolithic surveillance application that queries the case database, runs the outbreak detection algorithm, branches with if/else into alert or monitoring logic, and then triggers the response. If the surveillance data feed is temporarily unavailable, you'd need retry logic. If the system crashes after detecting an outbreak but before issuing the alert, cases could go unreported. Epidemiologists need a complete timeline of every surveillance run for retrospective analysis.

## The Solution

**You just write the disease monitoring, outbreak detection, alert routing, and public health response coordination logic. Conductor handles surveillance retries, intervention routing, and public health audit trails.**

Each stage of the surveillance pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running surveillance before detection, routing to alert or monitoring via SWITCH based on the detection outcome, always executing the response step regardless of which branch was taken, and maintaining a full audit trail of every surveillance cycle.

### What You Write: Workers

Surveillance data collection, outbreak analysis, intervention planning, and public notification workers each address one public health response function.

| Worker | Task | What It Does |
|---|---|---|
| **SurveillanceWorker** | `phw_surveillance` | Pulls baseline epidemiological data for a given disease and region |
| **DetectOutbreakWorker** | `phw_detect_outbreak` | Compares current case count against baseline to determine if an outbreak is occurring; returns "alert" or "monitor" |
| **AlertWorker** | `phw_alert` | Issues a public health alert for the affected region and disease with severity level |
| **MonitorWorker** | `phw_monitor` | Schedules continued surveillance with a next-check date when cases are elevated but below alert threshold |
| **RespondWorker** | `phw_respond` | Executes the public health response plan (contact tracing, resource deployment, public communications) |

### The Workflow

```
phw_surveillance
 │
 ▼
phw_detect_outbreak
 │
 ▼
SWITCH (phw_switch_ref)
 ├── alert: phw_alert
 ├── monitor: phw_monitor
 │
 ▼
phw_respond

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
