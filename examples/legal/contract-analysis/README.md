# Contract Analysis in Java with Conductor

## The Problem

A new vendor contract lands on the legal team's desk. You need to parse the document, extract key clauses (termination terms, liability caps, non-compete provisions), assess risk levels across each clause, and produce a consolidated summary for the business team to review before signing. Doing this manually across dozens of contracts per quarter leads to missed risk clauses and inconsistent analysis.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You just write the contract parsing, clause extraction, risk assessment, and summary generation logic. Conductor handles extraction retries, risk scoring sequencing, and contract review audit trails.**

Each worker handles one legal operation. Conductor manages the review pipeline, approval chains, deadline enforcement, and audit trail.

### What You Write: Workers

Clause extraction, risk identification, obligation mapping, and summary generation workers each analyze one dimension of contract language.

| Worker | Task | What It Does |
|---|---|---|
| **ParseWorker** | `cna_parse` | Parses the contract document, extracting page count (42) and section count (18) for downstream analysis |
| **ExtractWorker** | `cna_extract` | Extracts key clauses from the parsed contract. termination terms (90-day notice), liability caps, and clause inventory |
| **AnalyzeWorker** | `cna_analyze` | Evaluates risk across extracted clauses, flags high-risk items (e.g., non-compete clauses), and assigns an overall risk level (low/medium/high) |
| **SummarizeWorker** | `cna_summarize` | Generates a summary with a unique ID (SUM-694), consolidating parsed data, clause details, and identified risks into a single output |

### The Workflow

```
cna_parse
 │
 ▼
cna_extract
 │
 ▼
cna_analyze
 │
 ▼
cna_summarize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
