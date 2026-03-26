# Premium Calculation

Orchestrates premium calculation through a multi-stage Conductor workflow.

**Input:** `policyType`, `applicantAge`, `coverageAmount` | **Timeout:** 60s

## Pipeline

```
pmc_collect_factors
    │
pmc_calculate_base
    │
pmc_apply_modifiers
    │
pmc_finalize
```

## Workers

**ApplyModifiersWorker** (`pmc_apply_modifiers`)

Outputs `adjustedPremium`, `discounts`.

**CalculateBaseWorker** (`pmc_calculate_base`)

Outputs `basePremium`.

**CollectFactorsWorker** (`pmc_collect_factors`)

Reads `policyType`. Outputs `factors`.

**FinalizeWorker** (`pmc_finalize`)

Outputs `finalPremium`, `breakdown`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
