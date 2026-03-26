# DevSecOps Pipeline

A code commit needs security validation before deployment. The pipeline runs SAST (static analysis: 0 critical, 2 medium), SCA (dependency scan: 0 critical, 1 high vulnerability), container image scanning (0 critical vulnerabilities), and a final security gate that blocks the build if any critical findings exist.

## Workflow

```
dso_sast_scan ──> dso_sca_scan ──> dso_container_scan ──> dso_security_gate
```

Workflow `devsecops_pipeline_workflow` accepts `repository` and `commitSha`. Times out after `600` seconds.

## Workers

**SastScanWorker** (`dso_sast_scan`) -- runs static analysis. Reports `"Static analysis: 0 critical, 2 medium"`. Returns `sast_scanId` = `"SAST_SCAN-1356"`.

**ScaScanWorker** (`dso_sca_scan`) -- scans dependencies. Reports `"Dependency scan: 0 critical, 1 high vulnerability"`. Returns `sca_scan` = `true`.

**ContainerScanWorker** (`dso_container_scan`) -- scans the container image. Reports `"Image scan: 0 critical vulnerabilities"`. Returns `container_scan` = `true`.

**SecurityGateWorker** (`dso_security_gate`) -- evaluates all scan results. Reports `"Security gate PASSED -- no critical findings"`. Returns `security_gate` = `true`.

## Workflow Output

The workflow produces `sast_scanResult`, `security_gateResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `devsecops_pipeline_workflow` defines 4 tasks with input parameters `repository`, `commitSha` and a timeout of `600` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end security scanning pipeline and gate decision.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
