# Legal Case Management in Java with Conductor

## The Problem

A new client matter arrives at the firm. You need to intake the case and assign it a tracking number, assess its complexity to determine staffing needs, assign the right attorney, track the case through phases like discovery and resolution, and eventually close it with a recorded outcome (e.g., settled). Without a structured workflow, cases fall through the cracks, deadlines get missed, and attorneys are assigned unevenly.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the case intake, party management, deadline tracking, and case resolution logic. Conductor handles deadline tracking, party management retries, and case lifecycle audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Case intake, party management, deadline tracking, and disposition workers handle case lifecycle through independent operational steps.

| Worker | Task | What It Does |
|---|---|---|
| **IntakeWorker** | `lcm_intake` | Registers a new case in the system and generates a case ID (e.g., CASE-691) for downstream tracking |
| **AssessWorker** | `lcm_assess` | Evaluates the case and assigns a complexity level (low/medium/high) to guide attorney assignment and resource allocation |
| **AssignWorker** | `lcm_assign` | Assigns a qualified attorney to the case, returning an attorney ID (e.g., ATT-25) based on complexity and availability |
| **TrackWorker** | `lcm_track` | Monitors the case through its lifecycle phases (e.g., discovery, trial, resolution) and reports the current phase |
| **CloseWorker** | `lcm_close` | Closes the case with a recorded outcome (e.g., "settled", "dismissed", "verdict") and finalizes all case records |

### The Workflow

```
lcm_intake
 │
 ▼
lcm_assess
 │
 ▼
lcm_assign
 │
 ▼
lcm_track
 │
 ▼
lcm_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
