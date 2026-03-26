# Compliance Monitoring

Resources need continuous compliance checking against a framework. The pipeline scans resources, evaluates policies, and either logs a clean result or triggers auto-remediation based on the `autoRemediate` flag.

## Workflow

```
cpm_scan_resources ──> cpm_evaluate_policies ──> SWITCH
                                                   ├── compliant ──> cpm_log_compliant
                                                   └── violations ──> cpm_remediate
```

Workflow `compliance_monitoring_428` accepts `framework`, `scope`, and `autoRemediate`. Times out after `60` seconds.

## Workers

**ScanResourcesWorker** (`cpm_scan_resources`) -- scans resources for the specified compliance framework.

**EvaluatePoliciesWorker** (`cpm_evaluate_policies`) -- evaluates scanned resources against policy rules.

**LogCompliantWorker** (`cpm_log_compliant`) -- logs that all checks passed.

**RemediateWorker** (`cpm_remediate`) -- auto-remediates violations when the flag is set.

## Workflow Output

The workflow produces `resourcesScanned`, `overallStatus`, `complianceScore`, `violations` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `compliance_monitoring_428` defines 3 tasks with input parameters `framework`, `scope`, `autoRemediate` and a timeout of `60` seconds.

## Tests

2 tests verify the compliance check pipeline with both compliant and non-compliant outcomes.


## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
