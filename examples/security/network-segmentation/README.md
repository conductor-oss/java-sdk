# Network Segmentation

A VPC needs security zone isolation. The pipeline defines 4 zones (DMZ, app, data, mgmt), configures 28 firewall rules between zones, applies security group policies across the VPC, and verifies that no unauthorized cross-zone traffic can pass.

## Workflow

```
ns_define_zones ──> ns_configure_rules ──> ns_apply_policies ──> ns_verify_isolation
```

Workflow `network_segmentation_workflow` accepts `environment` and `segmentationType`. Times out after `600` seconds.

## Workers

**DefineZonesWorker** (`ns_define_zones`) -- reports `"Defined 4 zones: DMZ, app, data, mgmt"`. Returns `define_zonesId` = `"DEFINE_ZONES-1600"`.

**ConfigureRulesWorker** (`ns_configure_rules`) -- reports `"Configured 28 firewall rules between zones"`. Returns `configure_rules` = `true`.

**ApplyPoliciesWorker** (`ns_apply_policies`) -- reports `"Security group policies applied across VPC"`. Returns `apply_policies` = `true`.

**VerifyIsolationWorker** (`ns_verify_isolation`) -- reports `"Zone isolation verified: no unauthorized cross-zone traffic"`. Returns `verify_isolation` = `true`.

## Workflow Output

The workflow produces `define_zonesResult`, `verify_isolationResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `network_segmentation_workflow` defines 4 tasks with input parameters `environment`, `segmentationType` and a timeout of `600` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end network segmentation pipeline from zone definition through isolation verification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
