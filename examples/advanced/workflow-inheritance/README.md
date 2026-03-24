# Workflow Inheritance

A platform has multiple workflow variants that share 80% of their logic. The base order workflow handles standard orders; the express variant adds a priority flag and skips the approval step; the international variant adds customs clearance. Inheritance lets variants reuse the base definition and override only what differs.

## Pipeline

```
[wi_init]
     |
     v
[wi_validate]
     |
     v
[wi_process_standard]
     |
     v
[wi_finalize]
```

**Workflow inputs:** `requestId`, `data`

## Workers

**WiFinalizeWorker** (task: `wi_finalize`)

- Writes `finalized`

**WiInitWorker** (task: `wi_init`)

- Writes `initialized`

**WiProcessPremiumWorker** (task: `wi_process_premium`)

- Sets `result` = `"premium_processed"`
- Writes `result`, `sla`

**WiProcessStandardWorker** (task: `wi_process_standard`)

- Sets `result` = `"standard_processed"`
- Writes `result`, `sla`

**WiValidateWorker** (task: `wi_validate`)

- Writes `validatedData`, `valid`

---

**20 tests** | Workflow: `workflow_inheritance_standard_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
