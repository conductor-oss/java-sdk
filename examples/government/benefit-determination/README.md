# Benefit Determination

Orchestrates benefit determination through a multi-stage Conductor workflow.

**Input:** `applicantId`, `programType`, `income` | **Timeout:** 60s

## Pipeline

```
bnd_apply
    │
bnd_verify_eligibility
    │
bnd_calculate
    │
route_eligibility [SWITCH]
  ├─ eligible: bnd_notify_eligible
  └─ ineligible: bnd_notify_ineligible
```

## Workers

**ApplyWorker** (`bnd_apply`)

Reads `applicantId`, `programType`. Outputs `applicationId`.

**CalculateWorker** (`bnd_calculate`)

```java
int amount = "eligible".equals(eligibility) ? 850 : 0;
```

Reads `eligibility`. Outputs `benefitAmount`.

**NotifyEligibleWorker** (`bnd_notify_eligible`)

Reads `applicantId`, `benefit`. Outputs `notified`, `approved`.

**NotifyIneligibleWorker** (`bnd_notify_ineligible`)

Reads `applicantId`, `reason`. Outputs `notified`, `approved`.

**VerifyEligibilityWorker** (`bnd_verify_eligibility`)

```java
boolean eligible = income < 50000;
```

Reads `income`. Outputs `eligibility`, `reason`.

## Tests

**3 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
