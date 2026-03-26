# Compliance Insurance

Orchestrates compliance insurance through a multi-stage Conductor workflow.

**Input:** `companyId`, `regulatoryBody`, `compliancePeriod` | **Timeout:** 60s

## Pipeline

```
cpi_audit
    │
cpi_assess
    │
cpi_file_reports
    │
cpi_track
    │
cpi_certify
```

## Workers

**AssessWorker** (`cpi_assess`)

Outputs `assessment`, `remediationItems`, `riskLevel`.

**AuditWorker** (`cpi_audit`)

Reads `companyId`. Outputs `findings`.

**CertifyWorker** (`cpi_certify`)

Reads `companyId`. Outputs `complianceStatus`, `certificationId`.

**FileReportsWorker** (`cpi_file_reports`)

Reads `regulatoryBody`. Outputs `filingId`, `filed`.

**TrackWorker** (`cpi_track`)

Outputs `allResolved`, `resolvedCount`.

## Tests

**2 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
