# Smoke Testing in Java with Conductor : Endpoint Checks, Data Verification, Integration Tests, and Status Reporting

Orchestrates post-deployment smoke testing using [Conductor](https://github.com/conductor-oss/conductor). This workflow checks that critical API endpoints return 200 OK, verifies that the database has expected data (migrations ran, seed data present), tests integrations with external services (payment gateway reachable, email service connected), and reports the overall smoke test status with per-check results.

## Did the Deploy Actually Work?

You just deployed to staging. The CI pipeline says "success," but does the /health endpoint return 200? Can the API actually connect to the database? Is the payment gateway integration responding? Smoke tests answer these questions in 60 seconds. Before anyone files a bug report. Each check depends on the previous layer: there's no point testing database queries if the endpoints are down, and no point testing payment integration if the database has no data.

Without orchestration, you'd run a bash script with curl commands and grep for "200 OK." If the database check fails, you still run integration tests against a broken database and get confusing failures. There's no structured report of which checks passed and which failed, no retry when an endpoint takes a few seconds to warm up after deploy, and no audit trail of smoke test results across deployments.

## The Solution

**You write the health checks and integration tests. Conductor handles layered test sequencing, warm-up retries, and structured pass/fail reporting.**

Each layer of the smoke test is a simple, independent worker. The endpoint checker hits critical URLs (health, readiness, key API routes) and verifies HTTP status codes and response shapes. The data verifier connects to the database to confirm migrations ran, seed data exists, and key tables are queryable. The integration tester exercises connections to external services. Payment gateway, email provider, search index, cache layer. The status reporter aggregates all check results into a pass/fail verdict with details per check. Conductor executes them in strict sequence, retries endpoint checks when services need warm-up time after deploy, and provides a clear record of every smoke test run. ### What You Write: Workers

Three workers run layered smoke tests. Checking endpoint health, verifying database state, and reporting overall deployment status.

| Worker | Task | What It Does |
|---|---|---|
| **CheckEndpointsWorker** | `st_check_endpoints` | Hits all critical API endpoints and verifies they return healthy HTTP responses |
| **ReportStatusWorker** | `st_report_status` | Aggregates all check results into a final pass/fail verdict for the deployment |
| **VerifyDataWorker** | `st_verify_data` | Validates database connectivity and confirms sample queries return expected results |

the workflow and rollback logic stay the same.

### The Workflow

```
st_check_endpoints
 │
 ▼
st_verify_data
 │
 ▼
st_test_integrations
 │
 ▼
st_report_status

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
