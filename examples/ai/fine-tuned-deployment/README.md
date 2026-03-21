# Fine-Tuned Model Deployment in Java Using Conductor : Validate, Stage, Test, Promote

## Shipping a Fine-Tuned Model Safely

After fine-tuning a model, you can't just push it to production. The model artifacts need validation. are the weights complete, does the config match the base model, is the tokenizer present? Then the model needs to be deployed to a staging environment where acceptance tests verify that it produces sensible outputs and meets latency requirements. Only if all tests pass should the model be promoted to the production endpoint. If tests fail, the team needs to be notified with details about which tests broke.

This creates a conditional pipeline: validate, stage, test, then branch. promote on success, notify on failure. If the staging deployment times out, you need to retry it without re-running validation. If the test runner fails mid-suite, you need to know which tests passed before the failure. And you need a complete audit trail showing exactly which model version was promoted to production and when.

Without orchestration, this becomes a brittle deployment script where a staging timeout means starting over, test failures are discovered only by reading logs, and there's no record of which model version is serving traffic.

## The Solution

**You write the model validation, staging deployment, and acceptance testing logic. Conductor handles the conditional promotion, failure notifications, and observability.**

Each stage of the deployment pipeline is an independent worker. model validation, staging deployment, acceptance testing, production promotion, team notification. Conductor's `SWITCH` task routes to promotion or failure notification based on test results. If staging deployment times out, Conductor retries it automatically. Every deployment is tracked end-to-end, so you can see exactly when model `ft-gpt4-v3` was validated, staged, tested, and promoted.

### What You Write: Workers

Four workers plus a test runner cover the deployment lifecycle. model validation, staging deployment, test execution with a SWITCH gate, production promotion on pass, and team notification on failure.

| Worker | Task | What It Does |
|---|---|---|
| **DeployStagingWorker** | `ftd_deploy_staging` | Deploys the validated model to a staging endpoint. |
| **NotifyWorker** | `ftd_notify` | Sends notifications about pipeline status (completion or failure). |
| **PromoteProductionWorker** | `ftd_promote_production` | Promotes the model from staging to production. |
| **ValidateModelWorker** | `ftd_validate_model` | Validates fine-tuned model artifacts (weights, config, tokenizer). |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ftd_validate_model
 │
 ▼
ftd_deploy_staging
 │
 ▼
ftd_run_tests
 │
 ▼
SWITCH (route_ref)
 ├── true: ftd_promote_production
 └── default: ftd_notify
 │
 ▼
ftd_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
