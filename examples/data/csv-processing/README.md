# Csv Processing

A finance team receives a daily CSV export from a legacy ERP system. Column headers drift between runs, numeric fields arrive as locale-formatted strings with commas and currency symbols, and some rows contain obvious duplicates. The downstream warehouse expects clean, validated JSON records with consistent types.

## Pipeline

```
[cv_parse_csv]
     |
     v
[cv_validate_rows]
     |
     v
[cv_transform_fields]
     |
     v
[cv_generate_output]
```

**Workflow inputs:** `csvData`, `delimiter`, `hasHeader`

## Workers

**GenerateOutputWorker** (task: `cv_generate_output`)

Generates final output with summary statistics from transformed rows.

- Parses strings to `int`, formats output strings
- Reads `transformedRows`, `totalParsed`, `totalValid`. Writes `recordCount`, `summary`, `records`

**ParseCsvWorker** (task: `cv_parse_csv`)

Parses a CSV string into structured rows.

- Trims whitespace
- Reads `csvData`, `delimiter`. Writes `rows`, `headers`, `rowCount`

**TransformFieldsWorker** (task: `cv_transform_fields`)

Transforms validated rows: normalizes names, lowercases emails, uppercases departments, parses salaries to doubles.

- Lowercases strings, uppercases strings, trims whitespace, parses strings to `double`
- Reads `validRows`. Writes `rows`, `count`

**ValidateRowsWorker** (task: `cv_validate_rows`)

Validates parsed CSV rows: each row must have a non-empty name and an email containing "@".

- Reads `rows`. Writes `validRows`, `validCount`, `invalidCount`, `errors`

---

**36 tests** | Workflow: `csv_processing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
