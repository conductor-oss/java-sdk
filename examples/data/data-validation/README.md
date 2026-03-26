# Data Validation

An API gateway accepts JSON payloads from third-party partners, but the payloads frequently violate the agreed schema: missing required fields, wrong types, strings where numbers are expected. Before forwarding to the processing pipeline, each payload needs schema validation, type coercion, and a detailed error report for rejects.

## Pipeline

```
[vd_load_records]
     |
     v
[vd_check_required]
     |
     v
[vd_check_types]
     |
     v
[vd_check_ranges]
     |
     v
[vd_generate_report]
```

**Workflow inputs:** `records`, `schema`

## Workers

**CheckRangesWorker** (task: `vd_check_ranges`)

Checks value ranges: age must be 0-150, email must contain "@".

- Reads `records`. Writes `passedRecords`, `passedCount`, `errorCount`, `errors`

**CheckRequiredWorker** (task: `vd_check_required`)

Checks that required fields (name, email, age) are present and non-empty.

- Reads `records`. Writes `passedRecords`, `passedCount`, `errorCount`, `errors`

**CheckTypesWorker** (task: `vd_check_types`)

Checks that field types are correct: name must be String, age must be Number.

- Reads `records`. Writes `passedRecords`, `passedCount`, `errorCount`, `errors`

**GenerateReportWorker** (task: `vd_generate_report`)

Generates a validation summary report.

- Formats output strings
- Reads `totalRecords`, `requiredErrors`, `typeErrors`, `rangeErrors`, `validRecords`. Writes `summary`, `passRate`

**LoadRecordsWorker** (task: `vd_load_records`)

Loads records for validation.

- Reads `records`. Writes `records`, `count`

---

**46 tests** | Workflow: `data_validation` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
