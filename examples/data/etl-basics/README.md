# Etl Basics

A company's customer data lives in a CSV with untrimmed names, inconsistently cased emails, and amounts stored as strings. The data needs extraction from the source, transformation (trim whitespace, lowercase emails, parse `amount` to double), validation (reject records where `name` is empty, `email` is empty, or `amount` <= 0), loading to a destination file, and confirmation that the loaded count matches.

## Pipeline

```
[el_extract_data]
     |
     v
[el_transform_data]
     |
     v
[el_validate_output]
     |
     v
[el_load_data]
     |
     v
[el_confirm_load]
```

**Workflow inputs:** `jsonData`, `csvData`, `source`, `destination`, `rules`

## Workers

**ConfirmLoadWorker** (task: `el_confirm_load`)

Confirms that data loading completed successfully by verifying the output file exists, is non-empty, and the record count matches expectations.

- Deserializes from json
- Sets `status` = `"ETL_COMPLETE"`
- Reads `loadedCount`, `destination`. Writes `status`, `loadedCount`, `verified`, `fileSize`

**ExtractDataWorker** (task: `el_extract_data`)

Extracts data from a real source. Supports: - "jsonData" input: a JSON array string to parse - "csvData" input: a CSV string with headers to parse - "source" (file path): reads a JSON or CSV file from disk

- Trims whitespace, parses strings to `double`, parses strings to `int`, reads files from disk, deserializes from json
- Reads `source`, `jsonData`, `csvData`. Writes `error`, `records`, `recordCount`

**LoadDataWorker** (task: `el_load_data`)

Loads validated records into a destination file (JSON format). If destination is a file path, writes records to that file. If destination is not a valid path, uses a temp file.

- Writes to temp files, serializes to json
- Reads `records`, `destination`. Writes `loadedCount`, `destination`, `error`

**TransformDataWorker** (task: `el_transform_data`)

Transforms raw records: trims names, lowercases emails, parses amount strings to doubles.

- Lowercases strings, trims whitespace, parses strings to `double`
- Reads `rawRecords`. Writes `records`, `transformedCount`

**ValidateOutputWorker** (task: `el_validate_output`)

Validates transformed records: filters for records with non-empty name, email, and amount > 0.

- Reads `transformedRecords`. Writes `validRecords`, `validCount`, `invalidCount`

---

**47 tests** | Workflow: `etl_basics_wf` | Timeout: 120s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
