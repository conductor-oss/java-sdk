# ETL Workflow Templating in Java Using Conductor : Extract, Transform, Load, Verify

## Every ETL Job Is Extract-Transform-Load, but the Details Differ

Your company runs 50 ETL jobs. Each extracts from a different source (PostgreSQL, Salesforce API, S3 CSV files), transforms differently (currency conversion, deduplication, schema mapping), and loads to a different destination (Snowflake, Redshift, BigQuery). But the structure is always the same: extract, transform, load, verify. Without a template, each ETL job is built from scratch, duplicating retry logic, error handling, and verification.

Workflow templating means defining the ETL structure once (extract-transform-load-verify) and swapping in different worker implementations for different jobs. The template handles the orchestration. retries, failure routing, observability, while each job's workers handle the source-specific extraction, job-specific transformation, and destination-specific loading.

## The Solution

**You write the extract, transform, and load logic. Conductor handles the ETL template, batch retries, and verification tracking.**

`WtmExtractWorker` pulls data from the source system for the specified batch. `WtmTransformWorker` applies the configured transformation rules. cleaning, mapping, enriching. `WtmLoadWorker` writes the transformed data to the destination. `WtmVerifyWorker` confirms the load succeeded, checking row counts, running data quality assertions, and validating that the destination has the expected data. Conductor runs this template for every batch, retries failed extracts or loads, and records the row counts and verification results for every ETL run.

### What You Write: Workers

Four workers implement the reusable ETL template: source extraction, rule-based transformation, destination loading, and row-count verification, each swappable for different source/destination combinations.### The Workflow

```
wtm_extract
 │
 ▼
wtm_transform
 │
 ▼
wtm_load
 │
 ▼
wtm_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
