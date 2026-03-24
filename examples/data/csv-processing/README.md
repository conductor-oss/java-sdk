# CSV Processing in Java Using Conductor: Parsing, Validation, Transformation, and Clean Output

A partner sends you a 500MB CSV of customer records every Monday. Your API endpoint reads the whole file into memory, validates every row, transforms field formats, and writes clean output. all in a single synchronous request handler. At row 85,000, a salary field contains "N/A" instead of a number, `Double.parseDouble` throws, and the entire upload fails. The partner re-uploads. This time it gets to row 200,000 before your JVM runs out of heap and the API server crashes, taking down every other endpoint with it. You have no idea which rows were valid, which were rejected, or where the process actually failed, just a 502 in the partner's browser and an OOM in your logs. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You receive CSV files from partners, exports, or internal systems and need to turn them into clean, validated, normalized records. That means parsing raw text with configurable delimiters and header detection, rejecting rows with missing names or malformed emails, normalizing fields (trimming whitespace, standardizing case, parsing salary strings into numbers), and producing a final output with counts of what was accepted and rejected. Each step depends on the previous one. You can't validate without parsing first, and you can't transform rows that haven't been validated.

Without orchestration, you'd build a single class that reads the CSV, runs validation inline, transforms in the same pass, and writes output at the end. If the transformation logic throws an exception on an unexpected salary format, the entire file fails. There's no way to see which step rejected a row, no automatic retry if an upstream data source is temporarily unavailable, and adding a new validation rule or transformation means modifying tightly coupled code.

## The Solution

**You just write the CSV parsing, row validation, field transformation, and output generation workers. Conductor handles row-level tracking across parse-validate-transform stages, automatic retries, and visibility into accept/reject counts at each step.**

Each stage of the CSV pipeline is a simple, independent worker. The parser splits raw CSV text into structured rows using the configured delimiter and header mode. The validator checks each row for required fields and format constraints (non-empty name, valid email). The transformer normalizes field values. Title-casing names, lowercasing emails, uppercasing departments, parsing salary strings into doubles. The output generator assembles the clean records and computes summary statistics. Conductor executes them in sequence, passes rows between steps, retries if a step fails, and tracks exactly how many rows were parsed, validated, and transformed.

### What You Write: Workers

Four workers handle the CSV lifecycle: parsing raw text into rows, validating names and emails, normalizing field casing and types, and producing clean output with summary statistics.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateOutputWorker** | `cv_generate_output` | Generates final output with summary statistics from transformed rows. |
| **ParseCsvWorker** | `cv_parse_csv` | Parses a CSV string into structured rows. |
| **TransformFieldsWorker** | `cv_transform_fields` | Transforms validated rows: normalizes names, lowercases emails, uppercases departments, parses salaries to doubles. |
| **ValidateRowsWorker** | `cv_validate_rows` | Validates parsed CSV rows: each row must have a non-empty name and an email containing "@". |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks, the pipeline structure and error handling stay the same.

### The Workflow

```
cv_parse_csv
 │
 ▼
cv_validate_rows
 │
 ▼
cv_transform_fields
 │
 ▼
cv_generate_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
