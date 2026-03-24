# Data Format Normalizer in Java Using Conductor : Detect Format, Convert JSON/XML/CSV, Output Canonical Form

## Every Source System Speaks a Different Format

Your ERP sends XML, your CRM sends JSON, and your partner's FTP drop is CSV. Downstream analytics expects a single canonical format. Each integration point needs its own parser, and when a new source system joins with YAML or fixed-width files, you add another branch to a growing if/else chain.

The normalizer pattern detects the incoming format, routes to the correct converter, and produces the same canonical output structure regardless of how the data arrived. Adding a new format means adding one converter worker and one SWITCH case. not touching the rest of the pipeline.

## The Solution

**You write the format converters. Conductor handles detection routing, retries, and canonical output delivery.**

`NrmDetectFormatWorker` examines the raw input and source system metadata to determine whether the data is JSON, XML, or CSV. A `SWITCH` task routes to the matching converter: `NrmConvertJsonWorker` normalizes JSON payloads, `NrmConvertXmlWorker` transforms XML documents, and `NrmConvertCsvWorker` parses CSV rows. each producing the same canonical output structure. `NrmOutputWorker` emits the normalized result. Conductor's declarative routing means adding a new format is a one-worker, one-case change.

### What You Write: Workers

Five workers cover the normalization pipeline: format detection, and three format-specific converters (JSON, XML, CSV) plus canonical output delivery, each producing the same output structure regardless of source format.

### The Workflow

```
nrm_detect_format
 │
 ▼
SWITCH (nrm_switch_ref)
 ├── json: nrm_convert_json
 ├── xml: nrm_convert_xml
 ├── csv: nrm_convert_csv
 └── default: nrm_convert_json
 │
 ▼
nrm_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
