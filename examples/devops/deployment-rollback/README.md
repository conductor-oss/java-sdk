# Rolling Back a Failed Checkout Service Deployment

Your checkout-service deploy just spiked the error rate. You need to detect the failure,
identify the last stable version, roll back, and verify health -- all before customers
notice. This workflow automates the full rollback lifecycle from detection to verification.

## Workflow

```
service, reason
      |
      v
+--------------------+     +-----------------------+     +----------------------+     +-----------------------+
| rb_detect_failure  | --> | rb_identify_version   | --> | rb_rollback_deploy   | --> | rb_verify_rollback    |
+--------------------+     +-----------------------+     +----------------------+     +-----------------------+
  DETECT_FAILURE-1335       last stable: v2.4.3           rolled back to                error rate normalized
  error-rate-spike          deployed 3 days ago           previous version              service healthy
```

## Workers

**DetectFailureWorker** -- Takes `service` and `reason` inputs. Detects a checkout-service
failure triggered by an error-rate-spike. Returns `detect_failureId: "DETECT_FAILURE-1335"`.

**IdentifyVersionWorker** -- Finds the last stable version: v2.4.3, deployed 3 days ago.
Returns `identify_version: true`.

**RollbackDeployWorker** -- Rolls back the deployment to the previous stable version.
Returns `rollback_deploy: true`.

**VerifyRollbackWorker** -- Confirms the service is healthy after rollback with error rate
normalized. Returns `verify_rollback: true`.

## Tests

2 unit tests cover the rollback pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
