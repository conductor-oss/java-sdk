# Audit Logging

An admin deletes a user account. The system must capture the event with full actor and action context, enrich it with IP, device, session, and role information, write it to an immutable log, and verify the hash chain integrity to detect tampering.

## Workflow

```
al_capture_event ──> al_enrich_context ──> al_store_immutable ──> al_verify_integrity
```

Workflow `audit_logging_workflow` accepts `actor`, `action`, and `resource` as inputs. Times out after `60` seconds.

## Workers

**CaptureEventWorker** (`al_capture_event`) -- records `"admin@example.com delete user/12345"`. Returns `capture_eventId` = `"CAPTURE_EVENT-1481"` and `success` = `true`.

**EnrichContextWorker** (`al_enrich_context`) -- adds IP, device, session, and role context to the audit entry. Returns `enrich_context` = `true`.

**StoreImmutableWorker** (`al_store_immutable`) -- writes the enriched event to an immutable audit log. Returns `store_immutable` = `true`.

**VerifyIntegrityWorker** (`al_verify_integrity`) -- verifies the hash chain to confirm log integrity is intact. Returns `verify_integrity` = `true`.

## Workflow Output

The workflow produces `capture_eventResult`, `verify_integrityResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `audit_logging_workflow` defines 4 tasks with input parameters `actor`, `action`, `resource` and a timeout of `60` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the complete audit logging pipeline from event capture through integrity verification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
