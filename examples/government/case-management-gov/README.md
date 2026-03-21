# Government Case Management in Java with Conductor : Intake, Investigation, Evaluation, and Resolution

## The Problem

You need to manage government cases from intake through resolution. A citizen or agency files a report, which opens a case. An investigator gathers evidence and interviews witnesses. An evaluator reviews the findings and assesses severity. A decision-maker renders a formal determination based on the evaluation. Finally, the case is closed with a timestamp and resolution record. Each step depends on the output of the previous one. you cannot evaluate without investigation findings, and you cannot decide without an evaluation.

Without orchestration, you'd build a monolithic service that tracks case state in a database, calls each downstream service in sequence, and handles failures inline. If the investigation service is slow or unavailable, you'd need retry logic and timeout handling. If the system crashes after investigation but before evaluation, you'd need to figure out where to resume. Regulators and FOIA requests demand a complete record of every case action, timing, and decision.

## The Solution

**You just write the case intake, investigation, findings evaluation, decision rendering, and case closure logic. Conductor handles assignment retries, investigation sequencing, and case lifecycle audit trails.**

Each stage of the case lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running them in sequence, passing investigation findings to the evaluator, feeding the evaluation into the decision step, retrying if any service is temporarily unavailable, and maintaining a complete audit trail of every case from open to close. ### What You Write: Workers

Case creation, assignment, investigation, and resolution workers track government cases through a structured lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **OpenCaseWorker** | `cmg_open_case` | Creates a new case record from the reporter's complaint, assigns a case ID and case type |
| **InvestigateWorker** | `cmg_investigate` | Gathers evidence, interviews witnesses, and documents findings with severity assessment |
| **EvaluateWorker** | `cmg_evaluate` | Reviews investigation findings and produces a formal evaluation with recommendations |
| **DecideWorker** | `cmg_decide` | Renders a formal decision (approve, deny, refer) based on the evaluation |
| **CloseWorker** | `cmg_close` | Closes the case with a resolution record and timestamp |

### The Workflow

```
cmg_open_case
 │
 ▼
cmg_investigate
 │
 ▼
cmg_evaluate
 │
 ▼
cmg_decide
 │
 ▼
cmg_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
