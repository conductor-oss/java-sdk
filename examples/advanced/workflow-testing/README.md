# Workflow Testing

A workflow definition needs automated testing before deployment. The testing pipeline loads the workflow, executes it with known test inputs, asserts that each task produced expected outputs, and generates a pass/fail report with detailed diffs for failures.

## Pipeline

```
[wft_setup]
     |
     v
[wft_execute]
     |
     v
[wft_assert]
     |
     v
[wft_teardown]
     |
     v
[wft_report]
```

**Workflow inputs:** `testSuite`, `workflowUnderTest`, `expectedOutput`

## Workers

**AssertWorker** (task: `wft_assert`)

Checks assertions against expected output.

- Filters with predicates
- Reads `actualOutput`, `expectedOutput`. Writes `assertions`, `allPassed`

**ExecuteWorker** (task: `wft_execute`)

Executes the workflow under test using the provided fixtures. Processes the testData from the fixtures and produces actual output based on the input data rather than hardcoded values.

- Reads `workflowUnderTest`, `fixtures`. Writes `actualOutput`, `executionTimeMs`

**ReportWorker** (task: `wft_report`)

Generates the test report summarizing the test suite run.

- Filters with predicates
- Reads `testSuite`, `allPassed`, `assertions`, `teardownClean`. Writes `report`

**SetupWorker** (task: `wft_setup`)

Prepares test fixtures for the test suite.

- Reads `testSuite`. Writes `fixtures`

**TeardownWorker** (task: `wft_teardown`)

Cleans up test fixtures after test execution.

- Writes `cleanedUp`, `resourcesReleased`

---

**49 tests** | Workflow: `wft_workflow_testing` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
