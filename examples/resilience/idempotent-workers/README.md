# Idempotent Workers

A payment system charges a customer and sends a confirmation email. If the worker completes but the network drops before Conductor receives the result, Conductor retries -- and the customer gets double-charged or receives duplicate emails. Both workers must produce identical results whether executed once or five times.

## Workflow

```
idem_charge ──> idem_send_email
```

Workflow `idempotent_workers_demo` accepts `orderId`, `amount`, and `email` as inputs.

## Workers

**ChargeWorker** (`idem_charge`) -- uses a `ConcurrentHashMap<String, Map<String, Object>>` called `chargeCache` keyed by `orderId`. On first call, processes the charge and stores the output map (containing `charged` = `true`, `orderId`, `amount`, `duplicate` = `false`) in the cache. On subsequent calls with the same `orderId`, returns the cached result without reprocessing. The `amount` input is parsed via `((Number) amountInput).doubleValue()`.

**SendEmailWorker** (`idem_send_email`) -- uses a `Set<String>` backed by `ConcurrentHashMap.newKeySet()` called `sentEmails`. The deduplication key is `orderId + ":" + email`. If the key already exists in the set, returns `sent` = `true` with `duplicate` = `true` without re-sending. Otherwise, adds the key to the set and returns `duplicate` = `false`.

## Workflow Output

The workflow produces `charged`, `chargeDuplicate`, `emailSent`, `emailDuplicate` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 2 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `idempotent_workers_demo` defines 2 tasks with input parameters `orderId`, `amount`, `email` and a timeout of `120` seconds.

## Workflow Definition Details

Workflow description: "Idempotent workers demo -- charge then send email, both handling duplicates safely.". Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Implementation Notes

All workers implement the `com.netflix.conductor.client.worker.Worker` interface. Input parameters are read from `task.getInputData()` and output is written to `result.getOutputData()`. Workers return `TaskResult.Status.COMPLETED` on success and `TaskResult.Status.FAILED` on failure. The workflow JSON definition in `src/main/resources/` declares the task graph, input wiring via `${ref.output}` expressions, and output parameters.

## Tests

7 tests verify first-time charge processing, duplicate charge detection, first-time email delivery, duplicate email suppression, and the full charge-then-email pipeline with idempotency across retries.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
