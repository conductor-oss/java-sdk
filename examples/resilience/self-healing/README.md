# Self-Healing Workflow

A service goes unhealthy at 3 AM -- connection pool exhausted, high error rate. Instead of paging an engineer, the system should detect the issue, diagnose the root cause, apply the fix, and retry the original operation. This workflow implements the full detect-diagnose-remediate-retry loop.

## Workflow

```
sh_health_check ──> SWITCH(healthy)
                      ├── "false" ──> sh_diagnose ──> sh_remediate ──> sh_retry_process
                      └── default ──> sh_process
```

Workflow `self_healing_demo` accepts `service` and `data` as inputs. The SWITCH task `health_switch_ref` evaluates `${health_ref.output.healthy}` as a string comparison (`"true"` or `"false"`).

## Workers

**HealthCheckWorker** (`sh_health_check`) -- reads `service` from input. When `service` equals `"broken-service"`, returns `healthy` = `"false"` (as a String for SWITCH compatibility) and `symptoms` = `"connection_timeouts"`. Otherwise returns `healthy` = `"true"`.

**DiagnoseWorker** (`sh_diagnose`) -- receives `service` and `symptoms` from the health check output. Returns `diagnosis` = `"connection_pool_exhausted"` and `action` = `"restart_connection_pool"`.

**RemediateWorker** (`sh_remediate`) -- receives `diagnosis` and `action` from the diagnosis step. Applies the recommended fix and returns `fixed` = `true`.

**RetryProcessWorker** (`sh_retry_process`) -- reads `data` from workflow input. Returns `result` = `"healed-" + data`, indicating the service was healed and processing succeeded after remediation.

**ProcessWorker** (`sh_process`) -- reads `data` from input. Returns `result` = `"processed-" + data`. Runs on the healthy (default) SWITCH branch.

## Workflow Output

The workflow produces `result`, `healed` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 5 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `self_healing_demo` defines 2 tasks with input parameters `service`, `data` and a timeout of `120` seconds.

## Tests

3 tests verify the healthy path (direct processing), the unhealthy path (diagnose-remediate-retry), and that the diagnosis and remediation values propagate correctly through the healing chain.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
