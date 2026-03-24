# Regulatory Reporting

Regulatory reporting workflow: collect data, validate, format, submit, and confirm.

**Input:** `reportId`, `reportType`, `reportingPeriod`, `entity` | **Timeout:** 60s

## Pipeline

```
reg_collect_data
    │
reg_validate
    │
reg_format
    │
reg_submit
    │
reg_confirm
```

## Workers

**CollectDataWorker** (`reg_collect_data`)

Reads `reportType`, `reportingPeriod`. Outputs `data`, `recordCount`.

**ConfirmWorker** (`reg_confirm`)

Reads `submissionId`. Outputs `accepted`, `receiptNumber`, `confirmedAt`.

**FormatWorker** (`reg_format`)

```java
String format = "CALL".equals(type) ? "XBRL" : "XML";
```

Reads `reportType`. Outputs `formattedReport`, `formatVersion`.

**SubmitWorker** (`reg_submit`)

Reads `reportType`. Outputs `submissionId`, `submittedAt`, `deadline`, `submittedBefore`.

**ValidateWorker** (`reg_validate`)

```java
if (data.get("totalAssets") == null) errors++;
```

Reads `data`. Outputs `validatedData`, `errorCount`, `passed`.

## Tests

**5 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
