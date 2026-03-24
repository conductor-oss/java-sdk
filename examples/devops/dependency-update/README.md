# Automated Dependency Updates with Test Verification

Your auth-service has 8 outdated dependencies and nobody wants to spend a day bumping
versions, running tests, and opening PRs. This workflow scans for outdated packages,
applies the updates (2 major, 3 minor, 3 patch), runs the full test suite to verify
nothing broke, and opens a pull request with the diff.

## Workflow

```
repository, updateType
         |
         v
+--------------------+     +------------------+     +----------------+     +----------------+
| du_scan_outdated   | --> | du_update_deps   | --> | du_run_tests   | --> | du_create_pr   |
+--------------------+     +------------------+     +----------------+     +----------------+
  SCAN_OUTDATED-1334        8 deps updated           142 tests passed       PR created with
  8 outdated found          2 major, 3 minor,        run_tests=true         dependency diff
                            3 patch
```

## Workers

**ScanOutdatedWorker** -- Scans the specified `repository` and finds 8 outdated dependencies
in auth-service. Returns `scan_outdatedId: "SCAN_OUTDATED-1334"`.

**UpdateDepsWorker** -- Applies the version bumps: 2 major, 3 minor, 3 patch updates.
Returns `update_deps: true`.

**RunTestsWorker** -- Runs the full test suite. All 142 tests pass after the update.
Returns `run_tests: true`.

**CreatePrWorker** -- Opens a pull request containing the dependency diff. Returns
`create_pr: true`.

## Tests

2 unit tests cover the dependency update pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
