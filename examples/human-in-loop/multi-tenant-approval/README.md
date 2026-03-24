# Multi Tenant Approval

Multi-Tenant Approval -- tenant-specific approval rules based on amount and tenant configuration.

**Input:** `tenantId`, `amount` | **Timeout:** 300s

**Output:** `processed`, `approvalLevel`, `tenantId`, `amount`

## Pipeline

```
mta_load_config
    │
approval_switch [SWITCH]
  ├─ manager: manager_approval_wait
  └─ executive: manager_approval_wait → executive_approval_wait
    │
mta_finalize
```

## Workers

**MtaFinalizeWorker** (`mta_finalize`): Worker for mta_finalize task -- finalizes the approval process.

Reads `approvalLevel`, `tenantId`. Outputs `processed`.

**MtaLoadConfigWorker** (`mta_load_config`): Worker for mta_load_config task -- loads tenant configuration and determines.

- "startup-co": autoApproveLimit=5000, levels=["manager"]
- "enterprise-corp": autoApproveLimit=1000, levels=["manager","executive"]
- "small-biz": autoApproveLimit=10000, levels=[]
- executive if 2+ levels

- `levelCount >= 2` &rarr; `"executive"`
- `levelCount == 1` &rarr; `"manager"`

Reads `amount`, `tenantId`. Outputs `approvalLevel`, `tenantId`, `amount`.

## Workflow Output

- `processed`: `${mta_finalize_ref.output.processed}`
- `approvalLevel`: `${mta_load_config_ref.output.approvalLevel}`
- `tenantId`: `${workflow.input.tenantId}`
- `amount`: `${workflow.input.amount}`

## Data Flow

**mta_load_config**: `tenantId` = `${workflow.input.tenantId}`, `amount` = `${workflow.input.amount}`
**approval_switch** [SWITCH]: `switchCaseValue` = `${mta_load_config_ref.output.approvalLevel}`
**mta_finalize**: `tenantId` = `${workflow.input.tenantId}`, `amount` = `${workflow.input.amount}`, `approvalLevel` = `${mta_load_config_ref.output.approvalLevel}`

## Tests

**19 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
