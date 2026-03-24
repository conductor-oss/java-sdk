# Deploying a Fine-Tuned Model: Validate, Stage, Test, Promote

After fine-tuning a 7B-parameter model, you cannot push it directly to production. The artifacts need checksum validation, a staging deployment, acceptance tests (latency p99, accuracy, toxicity, regression), and conditional promotion. If any test fails, the team gets notified with details instead of a silent broken endpoint.

## Workflow

```
modelId, modelVersion, baseModel
       │
       ▼
┌─────────────────────┐
│ ftd_validate_model  │  Check weights, config, tokenizer
└──────────┬──────────┘
           │  valid, checksum: "sha256:a1b2c3d4e5f6"
           ▼
┌─────────────────────┐
│ ftd_deploy_staging  │  Deploy to staging endpoint
└──────────┬──────────┘
           │  endpoint: "https://staging.models.internal/<modelId>"
           ▼
┌─────────────────────┐
│ ftd_run_tests       │  Run 4 acceptance tests
└──────────┬──────────┘
           │  allPassed: "true"/"false"
           ▼
      ┌─ SWITCH ─────────────────────────────┐
      │                                      │
   "true"                                 default
      │                                      │
      ▼                                      ▼
┌───────────────────────┐         ┌───────────────┐
│ ftd_promote_production│         │ ftd_notify    │
└───────────┬───────────┘         │ (tests failed)│
            │                     └───────────────┘
            ▼
┌───────────────┐
│ ftd_notify    │
│ (pipeline done)│
└───────────────┘
```

## Workers

**ValidateModelWorker** (`ftd_validate_model`) -- Takes `modelId`, `modelVersion`, and `baseModel`. Validates weights, config, and tokenizer files. Reports `checksum: "sha256:a1b2c3d4e5f6"`, `parameterCount: "7B"`, and `fileSize: "13.5GB"`.

**DeployStagingWorker** (`ftd_deploy_staging`) -- Constructs a staging endpoint URL as `"https://staging.models.internal/" + modelId`. Reports `status: "running"`, `gpuType: "A100"`, `replicas: 2`.

**RunTestsWorker** (`ftd_run_tests`) -- Runs 4 acceptance tests: `latency_p99` (threshold <200ms, actual 120ms), `accuracy_benchmark` (>90%, actual 94.2%), `toxicity_check` (<1%, actual 0.01%), `regression_suite` (48/48 passed). Returns `allPassed` as a String (`"true"`/`"false"`) for SWITCH compatibility. When `CONDUCTOR_OPENAI_API_KEY` is set, also calls `gpt-4o-mini` to generate a test report and appends it as `testReportDetails`. Collects failures into a separate list.

**PromoteProductionWorker** (`ftd_promote_production`) -- Builds the production URL as `"https://api.models.internal/" + modelId`. Reports `status: "live"`, `trafficPercent: 100`.

**NotifyWorker** (`ftd_notify`) -- Logs the `reason` and `modelId`. Used twice in the workflow: once for test failures ("Acceptance tests failed") and once for pipeline completion ("Pipeline complete").

## Tests

16 tests cover model validation, staging deployment, test execution with pass/fail paths, production promotion, and notification.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
