# Plagiarism Detection in Java with Conductor : Submission, Scanning, Comparison, Verdict Routing, and Reporting

## The Problem

You need to check student submissions for plagiarism before grades are assigned. The document must be ingested, scanned to extract textual fingerprints and n-grams, compared against a database of published papers, web content, and prior student submissions, and then a similarity score determines the verdict. If the submission is clean, it proceeds to grading; if flagged, it must be routed to the academic integrity office for review. Either way, an originality report is generated for the instructor's records.

Without orchestration, you'd chain document ingestion, text analysis, corpus comparison, and conditional notification in a single service. manually handling timeouts when the similarity engine takes too long on large documents, writing if/else routing for clean vs: flagged results, and logging every check to defend decisions during student appeals.

## The Solution

**You just write the document scanning, corpus comparison, clean/flagged verdict routing, and originality reporting logic. Conductor handles source comparison retries, similarity scoring, and detection audit trails.**

Each plagiarism-check concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, scan, compare), then routing via a SWITCH task to the correct outcome (clean or flagged), and finally generating the report, retrying if the similarity engine times out, maintaining an audit trail for every submission, and resuming from the last step if the process crashes.

### What You Write: Workers

Document ingestion, source comparison, similarity scoring, and report generation workers each tackle one stage of academic integrity checking.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `plg_submit` | Ingests the student's document submission and records it for processing |
| **ScanWorker** | `plg_scan` | Extracts textual fingerprints, n-grams, and structural features from the document |
| **CompareWorker** | `plg_compare` | Compares scan results against a corpus of sources and computes a similarity score and verdict |
| **HandleCleanWorker** | `plg_handle_clean` | Marks the submission as original and clears it for grading |
| **HandleFlaggedWorker** | `plg_handle_flagged` | Flags the submission for academic integrity review with the similarity score |
| **ReportWorker** | `plg_report` | Generates an originality report for the instructor's records |

### The Workflow

```
plg_submit
 │
 ▼
plg_scan
 │
 ▼
plg_compare
 │
 ▼
SWITCH (plg_switch_ref)
 ├── clean: plg_handle_clean
 └── default: plg_handle_flagged
 │
 ▼
plg_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
