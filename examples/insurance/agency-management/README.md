# Agency Management

Orchestrates agency management through a multi-stage Conductor workflow.

**Input:** `agentId`, `agentName`, `state` | **Timeout:** 60s

## Pipeline

```
agm_onboard
    │
agm_license
    │
agm_assign_territory
    │
agm_track
    │
agm_review
```

## Workers

**AssignTerritoryWorker** (`agm_assign_territory`)

Reads `agentId`. Outputs `territory`, `zipCodes`.

**LicenseWorker** (`agm_license`)

Reads `agentId`. Outputs `licenseNumber`, `expiresAt`.

**OnboardWorker** (`agm_onboard`)

Reads `agentId`. Outputs `onboarded`, `startDate`.

**ReviewWorker** (`agm_review`)

Reads `agentId`. Outputs `rating`, `bonusEligible`.

**TrackWorker** (`agm_track`)

Reads `agentId`. Outputs `performance`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
