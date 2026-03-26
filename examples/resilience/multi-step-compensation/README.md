# Multi-Step Compensation

A customer onboarding pipeline creates an account, sets up billing, and provisions cloud resources. When resource provisioning fails (quota exceeded, region unavailable), billing and the account must be undone in reverse order. Each undo worker receives the specific ID from its corresponding forward step.

## Workflow

```
Forward: msc_create_account ──> msc_setup_billing ──> msc_provision_resources
                                                              │ (fails when failAt="provision")
Compensation: msc_undo_provision ──> msc_undo_billing ──> msc_undo_account
```

The forward workflow `msc_forward_workflow` chains three steps. The compensation workflow `msc_compensation_workflow` runs undo steps in reverse order, receiving `resourceId`, `billingId`, and `accountId` from the forward workflow's output.

## Workers

**CreateAccountWorker** (`msc_create_account`) -- returns a deterministic `accountId` = `"ACCT-001"`. Always completes.

**SetupBillingWorker** (`msc_setup_billing`) -- receives `accountId` from the previous step. Returns a deterministic `billingId` = `"BILL-001"`. Always completes.

**ProvisionResourcesWorker** (`msc_provision_resources`) -- reads `failAt` from input. When `failAt` equals `"provision"`, returns `FAILED` with `error` = `"Provisioning failed"`. Otherwise returns `resourceId` = `"RES-001"`.

**UndoProvisionWorker** (`msc_undo_provision`) -- receives `resourceId` from input. Returns `undone` = `true`.

**UndoBillingWorker** (`msc_undo_billing`) -- receives `billingId` from input. Returns `undone` = `true`.

**UndoAccountWorker** (`msc_undo_account`) -- receives `accountId` from input. Returns `undone` = `true`.

## Project Structure

This example contains 6 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `?` defines 0 tasks with input parameters none and a timeout of `?` seconds.

## Tests

14 tests verify the forward happy path, provisioning failure, compensation execution in reverse order, and that each undo worker receives the correct ID from its corresponding forward step.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
