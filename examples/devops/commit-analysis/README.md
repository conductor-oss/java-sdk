# Mining Git History for Hotspots and Coupling Patterns

Your team ships features, fixes, and refactors every week but nobody tracks which modules
get the most bug fixes or which files always change together. This workflow parses recent
commits from a branch, classifies each by conventional-commit prefix, detects code health
patterns, and produces a risk report.

## Workflow

```
repoName, branch, days
          |
          v
+-------------------+     +----------------+     +----------------------+     +--------------+
| cma_parse_commits | --> | cma_classify   | --> | cma_detect_patterns  | --> | cma_report   |
+-------------------+     +----------------+     +----------------------+     +--------------+
  5 commits on main        features/fixes/        3 patterns detected         health: "good"
  last 30 days             refactors counted                                  risk: "low"
```

## Workers

**ParseCommitsWorker** -- Takes `branch` (default `"main"`) and `days` (default 30). Returns
5 hard-coded commits with hashes like `"abc123"`, messages following conventional-commit
format (`"feat: add search"`, `"fix: null pointer in auth"`, `"refactor: extract service
layer"`, etc.), file counts, and authors (`"alice"`, `"bob"`, `"carol"`).

**ClassifyWorker** -- Iterates over commits and counts by prefix: messages starting with
`"feat"` increment `features`, `"fix"` increments `fixes`, `"refactor"` increments
`refactors`. Returns the `summary` map alongside the classified commit list.

**DetectPatternsWorker** -- Identifies three patterns: a `"hotspot"` where the auth module
has frequent fixes, a `"contributor"` pattern where alice focuses on features and refactors,
and a `"coupling"` pattern where search and notifications often change together.

**ReportWorker** -- Takes the detected patterns and produces a report map with
`patternCount`, `health: "good"`, and `risk: "low"`.

## Tests

8 unit tests cover commit parsing, classification, pattern detection, and reporting.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
