# Data Export in Java Using Conductor : Parallel Multi-Format Export (CSV, JSON, Excel) and Bundling

## The Problem

Users and downstream systems need data in different formats. The finance team wants Excel with formatted columns. The integration partner needs JSON. The data analyst wants CSV for import into R or pandas. You need to export the same dataset to all three formats, do it as fast as possible (in parallel, not sequentially), and bundle the results into a single deliverable. If the Excel export fails due to a formatting issue, the CSV and JSON exports should still complete successfully.

Without orchestration, you'd write a single export method that generates CSV, then JSON, then Excel sequentially. Tripling the total export time. You'd manage thread pools manually to parallelize, add try/catch blocks around each format so one failure doesn't crash the others, and build your own logic to wait for all exports to finish before bundling. Adding a new export format (Parquet, XML, PDF) means modifying the parallelism and join logic every time.

## The Solution

**You just write the data preparation, CSV/JSON/Excel export, and bundle workers. Conductor handles parallel format generation via FORK_JOIN, independent retries per format, and automatic join-then-bundle sequencing.**

Each export format is a simple, independent worker. The data preparation worker queries the source and structures the data with headers. The CSV, JSON, and Excel workers each receive the same prepared data and produce their format-specific output. Conductor's `FORK_JOIN` runs all three exports simultaneously, waits for all to complete, and then the bundler packages them into a single archive. If one format fails, Conductor retries just that export. If the process crashes after two of three exports finish, Conductor resumes only the incomplete one. You get all of that, without writing a single line of parallelism or join logic.

### What You Write: Workers

Five workers handle the parallel export pipeline: preparing the data from a source query, generating CSV, JSON, and Excel formats simultaneously via FORK_JOIN, and bundling all formats into a single downloadable archive.

| Worker | Task | What It Does |
|---|---|---|
| **BundleExportsWorker** | `dx_bundle_exports` | Bundles all exported files into a single archive and uploads to destination. |
| **ExportCsvWorker** | `dx_export_csv` | Exports data to CSV format. |
| **ExportExcelWorker** | `dx_export_excel` | Exports data to Excel format. |
| **ExportJsonWorker** | `dx_export_json` | Exports data to JSON format. |
| **PrepareDataWorker** | `dx_prepare_data` | Prepares data for export by querying the data source. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
dx_prepare_data
 │
 ▼
FORK_JOIN
 ├── dx_export_csv
 ├── dx_export_json
 └── dx_export_excel
 │
 ▼
JOIN (wait for all branches)
dx_bundle_exports

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
