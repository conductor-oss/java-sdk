# Gdpr Consent

Orchestrates gdpr consent through a multi-stage Conductor workflow.

**Input:** `userId`, `consents` | **Timeout:** 60s

## Pipeline

```
gdc_present_options
    │
gdc_record_consent
    │
gdc_update_systems
    │
gdc_audit
```

## Workers

**AuditWorker** (`gdc_audit`)

Outputs `auditTrailId`, `immutable`.

**PresentOptionsWorker** (`gdc_present_options`)

Reads `userId`. Outputs `options`, `presentedAt`.

**RecordConsentWorker** (`gdc_record_consent`)

Reads `consents`, `userId`. Outputs `recorded`, `consentRecord`.

**UpdateSystemsWorker** (`gdc_update_systems`)

Outputs `systemsUpdated`, `systems`.

## Tests

**13 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
