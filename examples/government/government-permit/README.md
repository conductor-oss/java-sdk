# Government Permit

Orchestrates government permit through a multi-stage Conductor workflow.

**Input:** `applicantId`, `permitType`, `details` | **Timeout:** 60s

## Pipeline

```
gvp_apply
    │
gvp_validate
    │
gvp_review
    │
route_decision [SWITCH]
  ├─ approve: gvp_issue
  └─ deny: gvp_deny
```

## Workers

**ApplyWorker** (`gvp_apply`)

Reads `applicantId`, `permitType`. Outputs `application`.

**DenyWorker** (`gvp_deny`)

Reads `reason`. Outputs `denied`.

**IssueWorker** (`gvp_issue`)

Reads `applicantId`. Outputs `permitNumber`, `issued`.

**ReviewWorker** (`gvp_review`)

Outputs `decision`, `reason`.

**ValidateWorker** (`gvp_validate`)

Reads `application`. Outputs `validatedApp`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
