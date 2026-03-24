# Nested Switch

An order routing system uses two levels of SWITCH tasks. The outer switch routes by `region` (US, EU, or other). The inner switch routes by `tier` (premium or standard). Each combination has a dedicated handler worker.

## Workflow

```
SWITCH(region)
  ├── "US" ──> SWITCH(tier)
  │              ├── "premium" ──> ns_us_premium
  │              └── default ──> ns_us_standard
  ├── "EU" ──> SWITCH(tier)
  │              ├── "premium" ──> ns_eu_premium
  │              └── default ──> ns_eu_standard
  └── default ──> ns_other_region
                        │
                  ns_complete
```

Workflow `nested_switch_demo` accepts `region`, `tier`, and `amount`. Times out after `60` seconds.

## Workers

**NsUsPremiumWorker** (`ns_us_premium`), **NsUsStandardWorker** (`ns_us_standard`), **NsEuPremiumWorker** (`ns_eu_premium`), **NsEuStandardWorker** (`ns_eu_standard`), **NsOtherRegionWorker** (`ns_other_region`) -- each reports `region`, `tier`, and the handler name.

**NsCompleteWorker** (`ns_complete`) -- finalizes the order after region/tier routing.

## Workflow Output

The workflow produces `region`, `tier`, `done` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Task Configuration

- `ns_us_premium`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ns_us_standard`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ns_eu_premium`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ns_eu_standard`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ns_other_region`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30
- `ns_complete`: retryCount=2, retryLogic=FIXED, retryDelaySeconds=?, timeoutSeconds=60, responseTimeoutSeconds=30

These settings are declared in `task-defs.json` and apply independently to each task, controlling retry behavior, timeout detection, and backoff strategy without any changes to worker code.

## Project Structure

This example contains 6 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `nested_switch_demo` defines 2 tasks with input parameters `region`, `tier`, `amount` and a timeout of `60` seconds.

## Tests

4 tests verify routing for each region/tier combination and the completion step.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
