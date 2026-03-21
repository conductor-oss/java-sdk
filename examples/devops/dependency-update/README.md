# Dependency Update in Java with Conductor : Outdated Scanning, Version Bumping, Test Verification, and PR Creation

Orchestrates automated dependency updates using [Conductor](https://github.com/conductor-oss/conductor). This workflow scans a repository for outdated dependencies, updates them to the latest compatible versions (patch, minor, or major depending on the update type), runs the test suite to verify nothing breaks, and creates a pull request with the changes.

## The Stale Dependency Problem

Your project has 47 dependencies, and 12 of them are behind by at least one minor version. Three have known CVEs. Updating them manually means editing pom.xml or package.json, running tests, finding that one update broke something, reverting it, trying the next one, and eventually giving up because it's Friday. The dependencies stay stale until a security scanner flags them as critical, by which point you're four versions behind and the upgrade path involves breaking changes.

Without orchestration, you'd run `mvn versions:display-dependency-updates`, manually edit version numbers, run `mvn test`, and hope for the best. There's no audit trail of which dependencies were updated, whether tests passed before the PR was created, or which update introduced the failure. If the test run crashes halfway, you'd start over from scratch.

## The Solution

**You write the scanning and update logic. Conductor handles the scan-test-PR pipeline and ensures PRs only open after tests pass.**

Each stage of the dependency update pipeline is a simple, independent worker. The scanner checks the repository for outdated dependencies and identifies available updates based on the configured update type (patch-only for safety, minor for features, major for everything). The updater modifies the dependency manifest files to the target versions. The test runner executes the full test suite against the updated dependencies to catch regressions before anything gets merged. The PR creator opens a pull request with the dependency changes, a summary of what was updated and from/to versions, and the test results. Conductor executes them in strict sequence, ensures the PR is only created if tests pass, retries if the package registry is temporarily unavailable, and tracks how many dependencies were scanned, updated, and verified. ### What You Write: Workers

Three workers automate the dependency update cycle. Scanning for outdated packages, bumping versions, and creating pull requests after tests pass.

| Worker | Task | What It Does |
|---|---|---|
| **CreatePrWorker** | `du_create_pr` | Opens a pull request with the dependency changes and a summary diff of updated versions |
| **ScanOutdatedWorker** | `du_scan_outdated` | Scans the repository for outdated dependencies and identifies available updates |
| **UpdateDepsWorker** | `du_update_deps` | Updates dependency manifest files to the target versions (2 major, 3 minor, 3 patch) |

the workflow and rollback logic stay the same.

### The Workflow

```
du_scan_outdated
 │
 ▼
du_update_deps
 │
 ▼
du_run_tests
 │
 ▼
du_create_pr

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
