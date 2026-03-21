# Code Review AI in Java with Conductor : Parallel Security, Quality, and Style Analysis of Pull Requests

## Reviewing Code Across Three Dimensions Simultaneously

Manual code reviews are slow and inconsistent. Security vulnerabilities, code quality issues, and style violations each require different expertise, yet reviewers often focus on one dimension and miss the others. Running these checks sequentially wastes time when they are independent of each other. a security scan does not need to wait for the style check to finish.

This workflow parses the PR diff once, then fans out to three parallel analysis branches: security (detecting vulnerabilities like weak hash algorithms), quality (identifying complexity, duplication, and maintainability issues), and style (checking formatting and naming conventions). The FORK_JOIN task runs all three simultaneously and waits for all to complete. The report worker then merges findings from all three branches into a single review with total counts, per-category breakdowns, and an overall verdict.

## The Solution

**You just write the diff-parsing, security/quality/style analysis, and report-generation workers. Conductor handles the parallel fan-out and the merged verdict.**

Five workers handle the review pipeline. diff parsing, security checking, quality checking, style checking, and report generation. The diff parser extracts the file list and lines changed. The three analysis workers run in parallel via FORK_JOIN, the security checker looks for vulnerabilities (e.g., MD5 usage in `crypto.js`), the quality checker evaluates complexity and patterns, and the style checker enforces conventions. After all three complete, the report worker aggregates findings and renders the verdict. Conductor manages the parallel fan-out and join automatically.

### What You Write: Workers

ParseDiffWorker extracts file changes, then SecurityCheckWorker, QualityCheckWorker, and StyleCheckWorker run in parallel via FORK_JOIN, and ReportWorker merges the findings into a unified verdict.

| Worker | Task | What It Does |
|---|---|---|
| **ParseDiffWorker** | `cra_parse_diff` | Extracts the file list and line counts (additions/deletions) from the PR diff. |
| **QualityCheckWorker** | `cra_quality_check` | Analyzes code for complexity, duplication, and maintainability issues. |
| **ReportWorker** | `cra_report` | Aggregates findings from all checks into a unified review with total counts and a pass/fail verdict. |
| **SecurityCheckWorker** | `cra_security_check` | Scans for security vulnerabilities (e.g., weak hashing algorithms, injection risks). |
| **StyleCheckWorker** | `cra_style_check` | Checks code formatting, naming conventions, and style guideline adherence. |

### The Workflow

```
cra_parse_diff
 │
 ▼
FORK_JOIN
 ├── cra_security_check
 ├── cra_quality_check
 └── cra_style_check
 │
 ▼
JOIN (wait for all branches)
cra_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
