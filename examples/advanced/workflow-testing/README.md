# Workflow Testing in Java Using Conductor: Fixture Setup, Assertions, Teardown, and Report Generation

## The Problem

You have workflows in production and you need to verify they behave correctly after every change. Unit-testing individual workers is easy, they're just Java classes. But testing the full workflow, does the SWITCH route correctly? Does the FORK_JOIN merge outputs properly? Does the workflow handle a failed task and retry?, requires running the entire orchestration end-to-end.

You need to set up test data, trigger the workflow, wait for completion, check that every task produced the expected output, clean up, and report whether the test passed. Doing this manually after every change is unsustainable. Automating it means defining test suites with golden inputs and expected outputs, running them against the real Conductor engine, and producing pass/fail reports with detailed assertion results.

This example is for teams building CI/CD pipelines around Conductor workflows, validating integration environments before deployment, or running golden input/output validation suites against staging.

## The Solution

**You write the fixtures and assertions. Conductor handles the test orchestration, teardown sequencing, and report generation.**

`SetupWorker` prepares the test environment: creating mock databases, mock API endpoints, and test data fixtures. `ExecuteWorker` triggers the workflow under test with the golden input and captures its output. `AssertWorker` compares each field of the actual output against the expected output, producing a per-assertion pass/fail result with expected vs, actual values. `TeardownWorker` releases mock databases, API endpoints, and test data. `ReportWorker` computes the final test report from the assertion results, total assertions, passed count, overall pass/fail verdict, and teardown status. Conductor records every test run for regression tracking.

### What You Write: Workers

Five workers orchestrate the test lifecycle. Fixture setup, workflow execution against golden inputs, field-by-field assertion, resource teardown, and pass/fail report generation.

| Worker | Task | What It Does |
|---|---|---|
| **SetupWorker** | `wft_setup` | Creates test fixtures: mock database (localhost:5432/test_db), mock API endpoint (localhost:9090), and two test data records (alpha, beta). |
| **ExecuteWorker** | `wft_execute` | Runs the workflow under test with the fixtures and captures its actual output (status, processed count, per-record results). |
| **AssertWorker** | `wft_assert` | Compares actual vs. expected output field-by-field: checks `status` equality and `processed` count match. Returns a list of named assertions with expected/actual/passed for each. |
| **TeardownWorker** | `wft_teardown` | Releases all fixture resources: mockDb, mockApi, testData. Reports cleanup status. |
| **ReportWorker** | `wft_report` | Computes the test report from the assertion list: counts total and passed assertions, determines overall PASSED/FAILED verdict, includes teardown status. |

### The Workflow

The five tasks run sequentially. Each task's output feeds into the next task's input via Conductor's expression language.; no custom wiring code.

```
wft_setup (create fixtures: mockDb, mockApi, testData)
 |
 v
wft_execute (run workflow under test with fixtures, capture actualOutput)
 |
 v
wft_assert (compare actualOutput vs expectedOutput, produce per-assertion results)
 |
 v
wft_teardown (release fixtures, report cleanedUp status)
 |
 v
wft_report (compute report: total/passed assertions, PASSED/FAILED verdict)

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
