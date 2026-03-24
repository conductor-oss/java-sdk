# Switch with Default Case

A payment routing system uses a SWITCH task to route by `paymentMethod`. Known methods go to specific handlers (`stripe` for cards, `plaid` for bank transfers, `coinbase` for crypto). Unknown methods fall through to the default case for `manual_review`. A log task runs after all branches.

## Workflow

```
SWITCH(paymentMethod)
  ├── "card" ──> stripe handler
  ├── "bank" ──> plaid handler
  ├── "crypto" ──> coinbase handler
  └── default ──> manual_review handler
                    │
              dc_log
```

Workflow `default_case_demo` accepts `paymentMethod`. Times out after `60` seconds.

## Workers

**ProcessCardWorker** (`stripe`) -- handles card payments via Stripe.

**ProcessBankWorker** (`plaid`) -- handles bank transfers via Plaid.

**ProcessCryptoWorker** (`coinbase`) -- handles cryptocurrency via Coinbase.

**UnknownMethodWorker** (`manual_review`) -- handles unrecognized methods with `needsHuman` = `true`.

**LogWorker** (`dc_log`) -- logs the processing result for all payment methods.

## Workflow Output

The workflow produces `method`, `logged` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `dc_process_card`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_process_bank`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_process_crypto`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_unknown_method`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `dc_log`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `default_case_demo` defines 2 tasks with input parameters `paymentMethod` and a timeout of `60` seconds.

## Tests

19 tests verify routing for each known payment method, default case routing for unknown methods, and post-switch logging.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
