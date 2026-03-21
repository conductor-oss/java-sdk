# Implementing Data Classification in Java with Conductor : Data Store Scanning, PII Detection, Sensitivity Classification, and Label Application

## The Problem

You need to know what sensitive data lives in your systems before you can protect it. Across dozens of databases, data lakes, and file stores, there are tables and columns containing Social Security numbers, email addresses, payment card numbers, health records, and other regulated data. often with column names like `field_7` or `user_data` that give no indication of what they contain. Regulations (GDPR, CCPA, HIPAA, PCI-DSS) require you to know where this data is, classify it by sensitivity, and apply appropriate protection. Without a complete inventory, you cannot enforce encryption, retention, or access control policies consistently.

Without orchestration, data classification is a manual, one-time project that is out of date before it finishes. A security analyst connects to each database, exports column metadata, eyeballs it for patterns that look like PII, adds notes to a spreadsheet, and then tries to get the data catalog team to apply labels. New tables appear daily and nobody rescans. When a privacy regulator asks "where do you store SSNs?", the answer is "we think we know, but we are not confident." Building this as a script means a timeout on one large database kills the entire scan, and there is no record of which stores were actually classified.

## The Solution

**You just write the PII detection rules and catalog labeling calls. Conductor handles the ordered scan-to-label pipeline, retries when database connections time out, and an audit trail proving which stores were scanned and what classifications were applied.**

Each classification step is a simple, independent worker. one scans data stores for schema and column metadata, one runs pattern matching and ML-based detection to identify PII fields, one assigns sensitivity levels (public, internal, sensitive, restricted) based on the detected data types and regulatory context, one applies the classification labels to the data catalog. Conductor takes care of executing them in strict order so no labels are applied without a complete PII detection pass, retrying if a database connection times out during scanning, and maintaining a complete audit trail that shows exactly which stores were scanned, what PII was detected, and what classifications were assigned, essential for demonstrating compliance to regulators. ### What You Write: Workers

Four workers form the classification pipeline: ScanDataStoresWorker extracts schema metadata, DetectPiiWorker identifies sensitive fields like SSNs and emails, ClassifyWorker assigns sensitivity levels based on regulatory context, and ApplyLabelsWorker writes classifications to the data catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ScanDataStoresWorker** | `dc_scan_data_stores` | Connects to the target data store (database, S3 bucket, file share) and extracts schema metadata. table names, column names, data types, sample values, for downstream analysis |
| **DetectPiiWorker** | `dc_detect_pii` | Runs pattern matching and heuristic analysis across the scanned columns to identify PII fields. email addresses, Social Security numbers, phone numbers, credit card numbers, health record identifiers |
| **ClassifyWorker** | `dc_classify` | Assigns a sensitivity level (public, internal, sensitive, restricted) to each field based on the detected PII type and applicable regulatory context (GDPR, CCPA, HIPAA, PCI-DSS) |
| **ApplyLabelsWorker** | `dc_apply_labels` | Writes the classification labels to the data catalog so downstream systems (access control, DLP, retention) can enforce policies based on the assigned sensitivity levels |

the workflow logic stays the same.

### The Workflow

```
dc_scan_data_stores
 │
 ▼
dc_detect_pii
 │
 ▼
dc_classify
 │
 ▼
dc_apply_labels

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
