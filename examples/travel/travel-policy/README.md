# Travel Policy

Travel policy with SWITCH for compliant/exception.

**Input:** `employeeId`, `bookingType`, `amount`, `policyTier` | **Timeout:** 60s

## Pipeline

```
tpl_check
    │
policy_route [SWITCH]
  ├─ compliant: tpl_compliant
  └─ exception: tpl_exception
    │
tpl_process
```

## Workers

**CheckWorker** (`tpl_check`)

```java
String result = amount <= limit ? "compliant" : "exception";
```

Reads `amount`, `bookingType`, `policyTier`. Outputs `complianceResult`, `reason`.

**CompliantWorker** (`tpl_compliant`)

Reads `bookingType`. Outputs `autoApproved`.

**ExceptionWorker** (`tpl_exception`)

Reads `bookingType`, `reason`. Outputs `exceptionApproved`, `approvedBy`.

**ProcessWorker** (`tpl_process`)

Reads `employeeId`. Outputs `processed`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
