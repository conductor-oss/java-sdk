# Zero Trust Verification

Every access request is verified regardless of network location. The pipeline verifies the user's identity (MFA verified, trust score 95), assesses device compliance (LAPTOP-A1B2C3: compliant, patched, encrypted), evaluates context (corporate network, business hours, normal behavior), and enforces the policy with a composite trust score of 91.

## Workflow

```
zt_verify_identity ──> zt_assess_device ──> zt_evaluate_context ──> zt_enforce_policy
```

Workflow `zero_trust_verification_workflow` accepts `userId`, `deviceId`, and `requestedResource`. Times out after `30` seconds for low-latency access decisions.

## Workers

**VerifyIdentityWorker** (`zt_verify_identity`) -- verifies user identity. Reports `"engineer-01: MFA verified, trust score 95"`. Returns `verify_identityId` = `"VERIFY_IDENTITY-1366"`.

**AssessDeviceWorker** (`zt_assess_device`) -- evaluates device posture. Reports `"LAPTOP-A1B2C3: compliant, patched, encrypted"`. Returns `assess_device` = `true`.

**EvaluateContextWorker** (`zt_evaluate_context`) -- checks contextual signals. Reports `"Corporate network, business hours, normal behavior"`. Returns `evaluate_context` = `true`.

**EnforcePolicyWorker** (`zt_enforce_policy`) -- makes the access decision. Reports `"Access GRANTED -- composite trust score: 91"`. Returns `enforce_policy` = `true`.

## Workflow Output

The workflow produces `verify_identityResult`, `enforce_policyResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `zero_trust_verification_workflow` defines 4 tasks with input parameters `userId`, `deviceId`, `requestedResource` and a timeout of `30` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end zero trust verification pipeline from identity through policy enforcement.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
