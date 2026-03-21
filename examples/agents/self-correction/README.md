# Self-Correction Agent in Java Using Conductor : Generate Code, Test, Diagnose, Fix

Self-Correction. generates code, runs tests, and if tests fail diagnoses and fixes the code before delivering. ## AI-Generated Code Needs Testing and Self-Repair

LLM-generated code works on the first try about 60-70% of the time. The remaining 30-40% has bugs. off-by-one errors, missing edge cases, incorrect API usage. A self-correcting agent doesn't just generate code; it tests the code, and if tests fail, it diagnoses what went wrong and fixes it.

This creates a conditional pipeline: generate, test, then branch. On success, deliver the code. On failure, diagnose (analyze the test output, identify the root cause. "Array index starts at 1 but should start at 0") and fix (generate corrected code targeting the specific issue). The fixed code then goes through delivery. Without orchestration, implementing this generate-test-fix loop with proper state management and failure routing requires careful branching logic.

## The Solution

**You write the code generation, testing, diagnosis, and fix logic. Conductor handles the pass/fail routing, conditional repair path, and delivery.**

`GenerateCodeWorker` produces code from the requirement specification. `RunTestsWorker` executes test cases against the generated code and returns pass/fail status with detailed test output. Conductor's `SWITCH` routes based on test results: passing tests go to `DeliverWorker` for direct delivery. Failing tests go through `DiagnoseWorker` (analyzing test failures to identify root causes) and `FixWorker` (generating corrected code targeting the specific issues) before reaching `DeliverWorker`. Conductor records the original code, test results, diagnosis, and fixes. showing exactly what was wrong and how it was corrected.

### What You Write: Workers

Four workers implement self-correction. Generating code, running tests, and on failure diagnosing the issue and applying a fix before delivery.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `sc_deliver` | Delivers the final code. Accepts the code, test pass count, and an optional wasFixed flag. |
| **DiagnoseWorker** | `sc_diagnose` | Diagnoses errors found during testing. Returns a deterministic diagnosis identifying the missing negative-number guard. |
| **FixWorker** | `sc_fix` | Fixes code based on a diagnosis. Returns deterministic fixed code with a negative input guard added. |
| **GenerateCodeWorker** | `sc_generate_code` | Generates code from a requirement description. Returns a deterministic fibonacci function implementation. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
sc_generate_code
 │
 ▼
sc_run_tests
 │
 ▼
SWITCH (test_result_switch_ref)
 ├── pass: sc_deliver
 ├── fail: sc_diagnose -> sc_fix -> sc_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
