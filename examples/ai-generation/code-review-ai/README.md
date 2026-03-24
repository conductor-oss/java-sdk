# Code Review AI: Parallel Security, Quality, and Style Analysis

A pull request touches authentication code, API handlers, and a cryptographic utility -- three files with very different risk profiles. Running security, quality, and style checks sequentially triples the review time. Worse, a single monolithic reviewer tends to miss domain-specific issues that a specialist would catch immediately (like a weak hash algorithm in a crypto utility).

This workflow parses the PR diff, fans out to three parallel specialist reviewers via FORK_JOIN, and compiles a unified report with a pass/fail verdict.

## Pipeline Architecture

```
prUrl, diff
    |
    v
cra_parse_diff           (files, linesChanged=142, hunks=8)
    |
    v
FORK_JOIN
    |           |              |
    v           v              v
cra_security  cra_quality    cra_style
_check        _check         _check
    |           |              |
    +-----+-----+------+------+
          |
          v
       JOIN
          |
          v
cra_report               (totalFindings, verdict, commentPosted)
```

## Worker: ParseDiff (`cra_parse_diff`)

Extracts the changed files from the PR URL. Returns `files: ["src/auth.js", "src/api/users.js", "src/utils/crypto.js"]`, `linesChanged: 142`, and `hunks: 8`. The file list and line count feed into the parallel analyzers.

## Worker: SecurityCheck (`cra_security_check`)

Scans for security vulnerabilities. Returns a single high-severity finding: `{severity: "high", file: "src/utils/crypto.js", line: 23, message: "Weak hash algorithm (MD5)"}`. Reports `issueCount: 1`. This finding alone may be enough to block the PR.

## Worker: QualityCheck (`cra_quality_check`)

Analyzes code quality. Returns two findings: a medium-severity `"Function exceeds 50 lines"` at `src/api/users.js:45` and a low-severity `"Unused variable"` at line 78 of the same file. Reports `issueCount: 2`.

## Worker: StyleCheck (`cra_style_check`)

Checks code style and formatting. Returns three findings (e.g., missing JSDoc comments, inconsistent formatting). Reports `issueCount: 3`.

## Worker: Report (`cra_report`)

Aggregates findings from all three reviewers. Computes `totalFindings` by summing the issue counts (1 + 2 + 3 = 6). Determines a `verdict` based on whether any high-severity issues exist. Posts a review comment to the PR and returns `commentPosted: true` with the original `prUrl`.

## Tests

5 tests cover diff parsing, all three parallel review types, and report compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
