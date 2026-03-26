# Network Monitoring

Orchestrates network monitoring through a multi-stage Conductor workflow.

**Input:** `networkSegment`, `checkType` | **Timeout:** 60s

## Pipeline

```
nmn_monitor
    │
nmn_detect_issues
    │
nmn_diagnose
    │
nmn_repair
    │
nmn_verify
```

## Workers

**DetectIssuesWorker** (`nmn_detect_issues`)

Outputs `issues`, `issueCount`.

**DiagnoseWorker** (`nmn_diagnose`)

Outputs `diagnosis`, `node`.

**MonitorWorker** (`nmn_monitor`)

Reads `networkSegment`. Outputs `metrics`.

**RepairWorker** (`nmn_repair`)

Reads `networkSegment`. Outputs `repairId`, `repaired`, `action`.

**VerifyWorker** (`nmn_verify`)

Reads `networkSegment`. Outputs `verified`, `latency`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
