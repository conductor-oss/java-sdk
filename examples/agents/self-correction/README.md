# Self-Correction: Generate Code, Test, Diagnose Failure, Fix

The agent generates code, runs tests (which deliberately fail with `"TypeError: Maximum call stack exceeded for negative input"`), and a SWITCH routes on the test result. On `"fail"`: the agent diagnoses the issue, applies a fix (`changesMade: ["Added negative input guard"]`), then delivers. On `"pass"`: delivers directly.

## Workflow

```
requirement -> sc_generate_code -> sc_run_tests
  -> SWITCH(pass: sc_deliver, fail: sc_diagnose -> sc_fix -> sc_deliver)
```

## Workers

**GenerateCodeWorker** (`sc_generate_code`) -- Produces initial `code`.

**RunTestsWorker** (`sc_run_tests`) -- Returns `testResult: "fail"` with `errors: ["TypeError: Maximum call stack exceeded for negative input"]`.

**DiagnoseWorker** (`sc_diagnose`) -- Analyzes errors and produces a `diagnosis`.

**FixWorker** (`sc_fix`) -- Returns `fixedCode` with `changesMade: ["Added negative input guard"]`.

**DeliverWorker** (`sc_deliver`) -- Delivers with `deliveryStatus: "success"`.

## Tests

40 tests cover generation, test execution, diagnosis, fixing, and delivery.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
