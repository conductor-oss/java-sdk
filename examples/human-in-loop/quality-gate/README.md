# Quality Gate in Java Using Conductor : Automated Test Suite, SWITCH on Pass/Fail, QA Engineer WAIT Sign-Off, and Production Deployment

## Deployments Need Automated Tests and Human QA Sign-Off

Before deploying to production, code must pass automated tests. If tests pass, a QA engineer must sign off via a WAIT task. If tests fail, deployment is blocked. The SWITCH task routes between the QA approval path and the failure path based on test results. This ensures no code ships without both automated and human verification.

## The Solution

**You just write the test-runner and deployment workers. Conductor handles the pass/fail routing, the QA sign-off hold, and the deploy retry.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

RunTestsWorker executes the test suite and reports pass/fail counts, and DeployWorker pushes to production, the SWITCH that blocks failed builds and the QA sign-off WAIT are handled by Conductor.

| Worker | Task | What It Does |
|---|---|---|
| **RunTestsWorker** | `qg_run_tests` | Runs the automated test suite. executes all tests and reports totalTests, passedTests, failedTests, and an allPassed flag that the SWITCH uses to gate deployment |
| *SWITCH* | `test_result_switch` | Checks `allPassed` from the test results: if false, terminates the workflow with "Automated tests failed. Deployment blocked."; if true (default), advances to QA sign-off and deployment | Built-in Conductor SWITCH + TERMINATE. no worker needed |
| *WAIT task* | `qa_signoff` | Pauses until a QA engineer reviews the test results and manually signs off on the release via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **DeployWorker** | `qg_deploy` | Deploys the application to production after both automated tests pass and the QA engineer signs off |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
qg_run_tests
 │
 ▼
SWITCH (test_result_switch_ref)
 ├── false: tests_failed_terminate
 └── default: qa_signoff -> qg_deploy

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
