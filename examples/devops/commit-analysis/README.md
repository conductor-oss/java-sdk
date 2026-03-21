# Commit Analysis in Java with Conductor : Parse, Classify, and Detect Patterns in Git History

## Understanding What Your Team Has Been Building

Raw git log output is a wall of hashes and messages. To understand development trends. whether the team is spending more time on bug fixes than features, whether refactoring is keeping pace with new code, or whether certain components are seeing unusual churn, you need to parse, classify, and aggregate commits systematically.

This workflow processes a repository's recent history in four steps. The parser extracts commits from the specified branch and time window. The classifier categorizes each commit by type (feature, bugfix, refactor, chore, etc.) and produces a type distribution summary. The pattern detector analyzes the classified commits for trends like "increasing bug density in module X" or "refactoring sprint in week 3." The reporter compiles patterns and classifications into a readable summary.

## The Solution

**You just write the commit-parsing, classification, pattern-detection, and reporting workers. Conductor handles the analysis pipeline and data routing.**

Four workers form the analysis pipeline. commit parsing, classification, pattern detection, and reporting. The parser reads the git history for the specified branch and time range. The classifier labels each commit and produces a summary distribution. The pattern detector examines the classified data for trends and anomalies. The reporter merges patterns and classifications into a final report. Conductor sequences the steps and routes commits, classifications, and patterns between them via JSONPath.

### What You Write: Workers

ParseCommitsWorker reads git history, ClassifyWorker labels each commit by type, DetectPatternsWorker finds trends like bug density spikes, and ReportWorker compiles the narrative analysis.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `cma_classify` | Labels each commit by type (feature, bugfix, refactor, chore) and produces a type distribution summary. |
| **DetectPatternsWorker** | `cma_detect_patterns` | Analyzes classified commits to identify development patterns and trends (e.g., bug density spikes, refactoring sprints). |
| **ParseCommitsWorker** | `cma_parse_commits` | Extracts commits from the specified branch and time window with hashes, authors, and messages. |
| **ReportWorker** | `cma_report` | Compiles patterns and classifications into a narrative analysis report. |

### The Workflow

```
cma_parse_commits
 │
 ▼
cma_classify
 │
 ▼
cma_detect_patterns
 │
 ▼
cma_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
