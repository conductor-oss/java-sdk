# Exclusive Join

Three vendor APIs (A, B, C) are queried in parallel for pricing. After all respond, a selection worker compares the results and picks the best offer. This demonstrates `FORK_JOIN` for parallel fan-out with a post-join aggregation step.

## Workflow

```
FORK ──┬── ej_vendor_a ──┐
       ├── ej_vendor_b ──┤
       └── ej_vendor_c ──┤
                          JOIN
                           │
                    ej_select_best
```

Workflow `exclusive_join_demo` accepts `query`. Times out after `60` seconds.

## Workers

**VendorAWorker** (`ej_vendor_a`) -- responds to the query with Vendor A's offer.

**VendorBWorker** (`ej_vendor_b`) -- responds to the query with Vendor B's offer.

**VendorCWorker** (`ej_vendor_c`) -- responds to the query with Vendor C's offer.

**SelectBestWorker** (`ej_select_best`) -- compares all three vendor responses and selects the best.

## Workflow Output

The workflow produces `winner`, `price` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `ej_vendor_a`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ej_vendor_b`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ej_vendor_c`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ej_select_best`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `exclusive_join_demo` defines 3 tasks with input parameters `query` and a timeout of `60` seconds.

## Tests

7 tests verify parallel vendor queries, response collection after join, and best-offer selection logic.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
