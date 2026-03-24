# Quality Gate

Quality Gate -- run automated tests, switch on results, wait for QA sign-off, then deploy.

**Timeout:** 300s

**Output:** `allPassed`, `totalTests`, `passedTests`, `failedTests`, `deployed`

## Pipeline

```
qg_run_tests
    │
test_result_switch [SWITCH]
  ├─ false: tests_failed_terminate
  └─ default: qa_signoff → qg_deploy
```

## Workers

**DeployWorker** (`qg_deploy`): Worker for qg_deploy task -- deploys the application after QA sign-off.

- deployed: true

Outputs `deployed`.

**RunTestsWorker** (`qg_run_tests`): Worker for qg_run_tests task -- runs automated tests and reports results.

- allPassed: true (all tests passed)
- totalTests: 42
- passedTests: 42
- failedTests: 0

Outputs `allPassed`, `totalTests`, `passedTests`, `failedTests`.

## Workflow Output

- `allPassed`: `${run_tests_ref.output.allPassed}`
- `totalTests`: `${run_tests_ref.output.totalTests}`
- `passedTests`: `${run_tests_ref.output.passedTests}`
- `failedTests`: `${run_tests_ref.output.failedTests}`
- `deployed`: `${deploy_ref.output.deployed}`

## Data Flow

**test_result_switch** [SWITCH]: `switchCaseValue` = `${run_tests_ref.output.allPassed}`

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
