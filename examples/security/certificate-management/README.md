# Certificate Management

An infrastructure with 87 certificates across all environments needs automated lifecycle management. The system must inventory all certificates, identify the 5 that are expiring within the renewal window, renew them, and distribute the renewed certificates to 12 endpoints.

## Workflow

```
cm_inventory ──> cm_assess_expiry ──> cm_renew ──> cm_distribute
```

Workflow `certificate_management_workflow` accepts `scope` and `renewalWindow`. Times out after `1200` seconds.

## Workers

**InventoryWorker** (`cm_inventory`) -- scans all environments and reports `"87 certificates found"`. Returns `inventoryId` = `"INVENTORY-1354"`.

**AssessExpiryWorker** (`cm_assess_expiry`) -- evaluates expiry dates and reports `"5 certificates expiring within renewal window"`. Returns `assess_expiry` = `true`.

**RenewWorker** (`cm_renew`) -- renews the expiring certificates. Reports `"5 certificates renewed successfully"`. Returns `renew` = `true`.

**DistributeWorker** (`cm_distribute`) -- distributes renewed certificates. Reports `"Renewed certificates distributed to 12 endpoints"`. Returns `distribute` = `true`.

## Workflow Output

The workflow produces `inventoryResult`, `distributeResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `certificate_management_workflow` defines 4 tasks with input parameters `scope`, `renewalWindow` and a timeout of `1200` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end certificate lifecycle from inventory through distribution.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
