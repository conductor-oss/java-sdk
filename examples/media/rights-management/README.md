# Rights Management

Orchestrates rights management through a multi-stage Conductor workflow.

**Input:** `assetId`, `licenseId`, `usageType`, `territory` | **Timeout:** 60s

## Pipeline

```
rts_check_license
    │
rts_verify_usage
    │
rts_track_royalties
    │
rts_generate_report
```

## Workers

**CheckLicenseWorker** (`rts_check_license`)

Reads `valid`. Outputs `valid`, `expirationDate`, `allowedUsages`, `royaltyRate`, `licenseType`.

**GenerateReportWorker** (`rts_generate_report`)

Reads `reportId`. Outputs `reportId`, `generatedAt`, `format`, `reportUrl`.

**TrackRoyaltiesWorker** (`rts_track_royalties`)

Reads `paymentDue`. Outputs `paymentDue`, `rightsHolder`.

**VerifyUsageWorker** (`rts_verify_usage`)

Reads `territory`. Outputs `territory`, `territoryRestriction`, `verifiedAt`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
