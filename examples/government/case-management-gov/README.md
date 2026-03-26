# Case Management Gov

Orchestrates case management gov through a multi-stage Conductor workflow.

**Input:** `caseType`, `reporterId`, `description` | **Timeout:** 60s

## Pipeline

```
cmg_open_case
    │
cmg_investigate
    │
cmg_evaluate
    │
cmg_decide
    │
cmg_close
```

## Workers

**CloseWorker** (`cmg_close`)

Reads `caseId`, `decision`. Outputs `closedAt`, `archived`.

**DecideWorker** (`cmg_decide`)

Reads `caseId`. Outputs `decision`, `actionPlan`.

**EvaluateWorker** (`cmg_evaluate`)

Outputs `evaluation`, `riskLevel`.

**InvestigateWorker** (`cmg_investigate`)

Reads `caseId`. Outputs `findings`.

**OpenCaseWorker** (`cmg_open_case`)

Reads `caseType`, `reporterId`. Outputs `caseId`, `openedAt`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
