# Parallel Test Suite Execution with Aggregated Results

Running unit, integration, and end-to-end tests sequentially wastes time when they have no
dependencies on each other. This workflow sets up the environment once, then fans out three
test suites in parallel using a FORK_JOIN, waits for all to finish, and aggregates the
combined results into a single coverage report.

## Workflow

```
suite, branch
      |
      v
+---------------+
| at_setup_env  |   buildId: BLD-200001
+---------------+
      |
      +------ FORK_JOIN -------+
      |            |           |
      v            v           v
+-----------+ +-----------+ +---------+
| at_run_   | | at_run_   | | at_run_ |
| unit      | | integrat. | | e2e     |
+-----------+ +-----------+ +---------+
  247 passed    18 passed     12 passed
  12s           45s           120s
      |            |           |
      +------ JOIN ------------+
      |
      v
+---------------------+
| at_aggregate_results|   totalPassed: 277, coverage: 87%
+---------------------+
```

## Workers

**SetupEnv** -- Takes a `branch` input and provisions the test environment. Returns
`buildId: "BLD-200001"` and `ready: true`.

**RunUnit** -- Runs the unit test suite against the build. Reports 247 passed, 0 failed, in
12 seconds.

**RunIntegration** -- Runs integration tests. Reports 18 passed, 0 failed, in 45 seconds.

**RunE2e** -- Runs end-to-end tests. Reports 12 passed, 0 failed, in 120 seconds.

**AggregateResults** -- Sums results from all three suites: `totalPassed` = 277,
`totalFailed` = 0, `coverage` = 87%.

## Tests

21 unit tests cover environment setup, each test runner, and aggregation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
