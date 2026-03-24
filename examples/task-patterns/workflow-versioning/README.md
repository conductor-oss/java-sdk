# Workflow Versioning in Java with Conductor

Run multiple versions of the same workflow side by side. version 1 does calculate-then-audit, version 2 adds a bonus step between them.

## The Problem

You need to evolve a workflow without breaking existing executions. Version 1 does a calculation (value * 2) then an audit. Version 2 adds a bonus step (+10) between calculation and audit. Both versions must coexist. In-flight v1 executions continue on v1, while new executions can use v2. You need to start workflows against a specific version and compare their outputs.

Without versioning, changing a workflow definition affects all executions immediately. Conductor's versioning lets you deploy v2 while v1 is still running, roll back to v1 if v2 has issues, and compare outputs between versions.

## The Solution

**You just write the calculation, bonus, and audit workers. Conductor handles running multiple workflow versions side by side with independent execution histories.**

This example registers two versions of the same workflow and runs them side by side. Version 1 runs `ver_calc` (multiplies the input value by 2) then `ver_audit` (marks the result as audited). Version 2 inserts a `ver_bonus` step between them that adds 10 to the calculated result before auditing. The example code registers both workflow definitions, starts executions against each version explicitly, and compares their outputs. V1 produces `value * 2`, v2 produces `(value * 2) + 10`. In-flight v1 executions continue running on v1 even after v2 is deployed. You can roll back to v1 at any time or run A/B comparisons between versions.

### What You Write: Workers

Three workers support side-by-side workflow versioning: VerCalcWorker performs a calculation, VerBonusWorker adds a bonus (used only in v2), and VerAuditWorker records the final result, the same worker pool serves both workflow versions simultaneously.

| Worker | Task | What It Does |
|---|---|---|
| **VerAuditWorker** | `ver_audit` | Audit worker for the versioned workflow. Takes the final result, marks it as audited, and passes it through. |
| **VerBonusWorker** | `ver_bonus` | Bonus worker for the versioned workflow. Takes a base result and adds 10. |
| **VerCalcWorker** | `ver_calc` | Calculation worker for the versioned workflow. Takes a numeric value and returns value * 2. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
Input -> VerAuditWorker -> VerBonusWorker -> VerCalcWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
