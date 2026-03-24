# Background Check

Background check with parallel FORK_JOIN for criminal, employment, education.

**Input:** `candidateName`, `candidateId` | **Timeout:** 60s

## Pipeline

```
bgc_consent
    │
    ┌──────────────┬────────────────┬───────────────┐
    │ bgc_criminal │ bgc_employment │ bgc_education │
    └──────────────┴────────────────┴───────────────┘
bgc_report
```

## Workers

**ConsentWorker** (`bgc_consent`)

Reads `candidateName`. Outputs `consented`, `consentDate`.

**CriminalWorker** (`bgc_criminal`)

Reads `candidateId`. Outputs `result`, `jurisdictions`.

**EducationWorker** (`bgc_education`)

Reads `candidateId`. Outputs `result`, `degree`.

**EmploymentWorker** (`bgc_employment`)

Reads `candidateId`. Outputs `result`, `employersChecked`.

**ReportWorker** (`bgc_report`)

Outputs `overallResult`, `reportId`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
