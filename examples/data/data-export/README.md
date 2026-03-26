# Data Export

A compliance team needs to export a filtered subset of transaction records into a specific format for a regulatory filing. The export requires selecting records by date range, formatting monetary values to two decimal places, packaging the output as a downloadable file, and logging the export for audit.

## Pipeline

```
[dx_prepare_data]
     |
     v
     +────────────────────────────────────────────────────────+
     | [dx_export_csv] | [dx_export_json] | [dx_export_excel] |
     +────────────────────────────────────────────────────────+
     [join]
     |
     v
[dx_bundle_exports]
```

**Workflow inputs:** `query`, `formats`, `destination`

## Workers

**BundleExportsWorker** (task: `dx_bundle_exports`)

Bundles all exported files into a single archive and uploads to destination.

- Writes `bundleUrl`, `exportCount`, `files`

**ExportCsvWorker** (task: `dx_export_csv`)

Exports data to CSV format.

- Formats output strings, uses java streams
- Writes `file`, `fileSize`, `rowCount`

**ExportExcelWorker** (task: `dx_export_excel`)

Exports data to Excel format.

- Formats output strings
- Writes `file`, `fileSize`, `sheetName`, `rowCount`

**ExportJsonWorker** (task: `dx_export_json`)

Exports data to JSON format.

- Formats output strings
- Writes `file`, `fileSize`, `recordCount`

**PrepareDataWorker** (task: `dx_prepare_data`)

Prepares data for export by querying the data source.

- Writes `data`, `headers`, `recordCount`

---

**25 tests** | Workflow: `data_export` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
