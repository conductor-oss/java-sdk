# Data Reconciliation in Java Using Conductor : Cross-System Comparison and Discrepancy Reporting

## The Problem

You have the same data in two systems. Orders in billing and orders in fulfillment, transactions in the ledger and transactions in the payment gateway, inventory in the warehouse system and inventory in the ERP. These systems should agree, but they drift. You need to fetch records from both sources, join them by a key field (order ID, transaction ID), identify records that match perfectly, records that exist in both but have different values (amount mismatches, status discrepancies), and records that exist in one system but not the other. The result is a reconciliation report showing exactly where the systems disagree.

Without orchestration, you'd write a script that connects to both databases, pulls records, builds hash maps for comparison, and prints results. If one data source is slow or temporarily unavailable, the script fails entirely. If the process crashes after fetching source A but before fetching source B, you'd refetch both. There's no record of reconciliation rate trends over time, and adding a third data source means rewriting the comparison logic.

## The Solution

**You just write the source fetching, record comparison, and discrepancy reporting workers. Conductor handles fetch-compare-report sequencing, retries when either data source is temporarily unavailable, and full tracking of match and mismatch counts.**

Each stage of the reconciliation is a simple, independent worker. Source A and Source B fetch workers each connect to their respective system and return records. The comparator joins records by the configured key field and classifies each as matched, mismatched, missing-in-A, or missing-in-B. The report generator computes the reconciliation rate and lists every discrepancy. Conductor executes them in sequence, retries if a data source is temporarily unavailable, and tracks exactly how many records were fetched, matched, and mismatched. ### What You Write: Workers

Four workers handle cross-system reconciliation: fetching records from source A (e.g., billing), fetching from source B (e.g., fulfillment), comparing them by key field to find matches and discrepancies, and generating a reconciliation report.

| Worker | Task | What It Does |
|---|---|---|
| **CompareRecordsWorker** | `rc_compare_records` | Compares records from two sources and identifies matches, mismatches, and missing records. |
| **FetchSourceAWorker** | `rc_fetch_source_a` | Fetches records from source A (billing system). |
| **FetchSourceBWorker** | `rc_fetch_source_b` | Fetches records from source B (fulfillment system). |
| **GenerateDiscrepancyReportWorker** | `rc_generate_discrepancy_report` | Generates a discrepancy report from comparison results. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
rc_fetch_source_a
 │
 ▼
rc_fetch_source_b
 │
 ▼
rc_compare_records
 │
 ▼
rc_generate_discrepancy_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
