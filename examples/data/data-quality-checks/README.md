# Data Quality Checks

A data warehouse team keeps finding null values in supposedly required columns, dates in the future, and negative quantities that violate business rules. They need a systematic quality gate that runs completeness, range, format, and consistency checks on every incoming batch, scoring each dimension and blocking loads that fall below threshold.

## Pipeline

```
[qc_load_data]
     |
     v
     +────────────────────────────────────────────────────────────────────────+
     | [qc_check_completeness] | [qc_check_accuracy] | [qc_check_consistency] |
     +────────────────────────────────────────────────────────────────────────+
     [join]
     |
     v
[qc_generate_report]
```

**Workflow inputs:** `records`

## Workers

**CheckAccuracyWorker** (task: `qc_check_accuracy`)

Checks data accuracy using real validation rules: - Email format: validates with RFC-compliant regex pattern - Status: must be one of the allowed values - Phone format (if present): validates common phone patterns Reports per-record accuracy issues.

- `VALID_STATUSES` = Set.of("active", "inactive", "pending")
- Rounds with `math.round()`, applies compiled regex, matches against regex
- Reads `records`. Writes `score`, `accurateCount`, `issues`

**CheckCompletenessWorker** (task: `qc_check_completeness`)

Checks data completeness by verifying required fields are present and non-empty. Also reports which specific records and fields are missing.

- `REQUIRED_FIELDS` = List.of("id", "name", "email", "status")
- Trims whitespace, rounds with `math.round()`
- Reads `records`. Writes `score`, `filledFields`, `totalFields`, `missingDetails`

**CheckConsistencyWorker** (task: `qc_check_consistency`)

Checks data consistency by detecting: - Duplicate IDs - Duplicate email addresses (uniqueness check) - Referential consistency (cross-field checks) Reports specific duplicate details.

- Lowercases strings, trims whitespace, rounds with `math.round()`
- Sets `type` = `"duplicate_id"`, `type` = `"duplicate_email"`
- Reads `records`. Writes `score`, `duplicates`, `duplicateDetails`

**GenerateReportWorker** (task: `qc_generate_report`)

Generates a quality report from the three check scores.

- Parses strings to `double`, rounds with `math.round()`
- Reads `completeness`, `accuracy`, `consistency`, `totalRecords`. Writes `overallScore`, `grade`, `totalRecords`

**LoadDataWorker** (task: `qc_load_data`)

Loads incoming records for quality checks.

- Reads `records`. Writes `records`, `count`

---

**39 tests** | Workflow: `data_quality_checks` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
