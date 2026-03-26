# Financial Audit

Financial audit: define scope, collect evidence, test controls, generate report, remediate.

**Input:** `auditId`, `entityName`, `auditType`, `fiscalYear` | **Timeout:** 60s

## Pipeline

```
fau_define_scope
    │
fau_collect_evidence
    │
fau_test_controls
    │
fau_generate_report
    │
fau_remediate
```

## Workers

**CollectEvidenceWorker** (`fau_collect_evidence`)

Outputs `evidenceCount`, `documentsReviewed`, `interviewsConducted`, `samplesCollected`.

**DefineScopeWorker** (`fau_define_scope`)

Reads `auditId`, `auditType`, `entityName`. Outputs `scopeAreas`, `materialityThreshold`, `riskLevel`.

**GenerateReportWorker** (`fau_generate_report`)

Reads `entityName`. Outputs `reportId`, `opinion`.

**RemediateWorker** (`fau_remediate`)

Reads `findingsCount`. Outputs `remediationPlanId`, `actionsCreated`, `targetCompletionDate`.

**TestControlsWorker** (`fau_test_controls`)

Reads `evidenceItems`. Outputs `controlEffectiveness`, `findingsCount`, `findings`, `controlsTestedCount`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
