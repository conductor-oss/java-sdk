# Data Reconciliation

At the end of each business day, the payments team needs to reconcile transactions recorded in their system against the bank's settlement file. Amounts must match to the cent, missing entries on either side must be flagged, and discrepancies need classification before anyone can sign off on the day's books.

## Pipeline

```
[rc_fetch_source_a]
     |
     v
[rc_fetch_source_b]
     |
     v
[rc_compare_records]
     |
     v
[rc_generate_discrepancy_report]
```

**Workflow inputs:** `sourceA`, `sourceB`, `keyField`

## Workers

**CompareRecordsWorker** (task: `rc_compare_records`)

Compares records from two sources and identifies matches, mismatches, and missing records.

- Reads `recordsA`, `recordsB`. Writes `matched`, `mismatched`, `missingInA`, `missingInB`, `matchedCount`, `mismatchedCount`

**FetchSourceAWorker** (task: `rc_fetch_source_a`)

Fetches records from source A (billing system).

- Writes `records`, `recordCount`

**FetchSourceBWorker** (task: `rc_fetch_source_b`)

Fetches records from source B (fulfillment system).

- Writes `records`, `recordCount`

**GenerateDiscrepancyReportWorker** (task: `rc_generate_discrepancy_report`)

Generates a discrepancy report from comparison results.

- Formats output strings
- Writes `reconciliationRate`, `discrepancies`

---

**15 tests** | Workflow: `data_reconciliation` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
