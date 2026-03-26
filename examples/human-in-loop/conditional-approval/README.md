# Conditional Approval

Conditional Approval Routing -- routes approval requests to different approval chains based on amount tier.

**Input:** `requestId`, `amount`, `requester` | **Timeout:** 300s

**Output:** `requestId`, `amount`, `tier`, `processed`

## Pipeline

```
car_classify
    │
route_approval [SWITCH]
  ├─ low: mgr_only_approval
  ├─ medium: mgr_med_approval → dir_med_approval
  └─ high: mgr_hi_approval → dir_hi_approval → vp_hi_approval
    │
car_process
```

## Workers

**ClassifyWorker** (`car_classify`): Worker for car_classify — classifies a request amount into a tier.

- amount < 1000:  "low"   (manager approval only)
- 1000 <= amount < 10000: "medium" (manager + director)
- amount >= 10000: "high" (manager + director + VP)

- `amount >= 10000` &rarr; `"high"`
- `amount >= 1000` &rarr; `"medium"`

Reads `amount`. Outputs `tier`.

**ProcessWorker** (`car_process`): Worker for car_process — marks a request as processed after all approvals.

Reads `requestId`, `tier`. Outputs `processed`.

## Workflow Output

- `requestId`: `${workflow.input.requestId}`
- `amount`: `${workflow.input.amount}`
- `tier`: `${class_ref.output.tier}`
- `processed`: `${proc_ref.output.processed}`

## Data Flow

**car_classify**: `amount` = `${workflow.input.amount}`
**route_approval** [SWITCH]: `switchCaseValue` = `${class_ref.output.tier}`
**car_process**: `requestId` = `${workflow.input.requestId}`, `tier` = `${class_ref.output.tier}`

## Tests

**17 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
