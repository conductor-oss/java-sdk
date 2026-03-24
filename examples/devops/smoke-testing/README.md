# Post-Deploy Smoke Tests: Endpoints, Data, and Integrations

After deploying order-service, you need to verify it actually works -- not just that the pod
is running. This workflow checks all 8 endpoints, verifies database connectivity with sample
queries, confirms all 3 downstream services are reachable, and generates a pass/fail report.

## Workflow

```
service, environment
       |
       v
+----------------------+     +------------------+     +-------------------------+     +---------------------+
| st_check_endpoints   | --> | st_verify_data   | --> | st_test_integrations    | --> | st_report_status    |
+----------------------+     +------------------+     +-------------------------+     +---------------------+
  CHECK_ENDPOINTS-1341        database connectivity    all 3 downstream              smoke test PASSED
  all 8 endpoints             and sample queries       services reachable            deployment verified
  responding                  verified
```

## Workers

**CheckEndpointsWorker** -- Checks all 8 endpoints for order-service. Returns
`check_endpointsId: "CHECK_ENDPOINTS-1341"`.

**VerifyDataWorker** -- Verifies database connectivity and runs sample queries. Returns
`verify_data: true`.

**TestIntegrationsWorker** -- Confirms all 3 downstream services are reachable. Returns
`test_integrations: true`.

**ReportStatusWorker** -- Generates the final smoke test report: PASSED, deployment verified.
Returns `report_status: true`.

## Tests

2 unit tests cover the smoke testing pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
