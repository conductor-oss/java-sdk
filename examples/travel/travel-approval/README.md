# Travel Approval

Travel approval with SWITCH for auto/manager approval.

**Input:** `employeeId`, `destination`, `purpose`, `estimatedCost` | **Timeout:** 60s

## Pipeline

```
tva_submit
    │
tva_estimate
    │
approval_route [SWITCH]
  ├─ auto_approve: tva_auto_approve
  └─ manager: tva_manager_approve
```

## Workers

**AutoApproveWorker** (`tva_auto_approve`): Auto-approves travel requests under threshold. Real threshold logic.

```java
boolean autoApproved = cost <= threshold;
String needsManagerApproval = autoApproved ? "false" : "true";
```

Reads `estimatedCost`, `threshold`. Outputs `autoApproved`, `needsManagerApproval`.

**EstimateWorker** (`tva_estimate`): Estimates travel cost. Real cost breakdown computation.

```java
double hotel = days * 200;
double meals = days * 75;
```

Reads `days`, `destination`. Outputs `estimatedCost`, `flightCost`, `hotelCost`, `mealsCost`.

**ManagerApproveWorker** (`tva_manager_approve`): Manager approval for travel over threshold.

```java
String approver = cost > 20000 ? "VP-Travel" : "Manager";
```

Reads `estimatedCost`. Outputs `managerApproved`, `approver`, `approvedAt`.

**SubmitWorker** (`tva_submit`): Submits travel request. Real request validation.

Reads `destination`, `reason`. Outputs `requestId`, `submitted`, `submittedAt`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
