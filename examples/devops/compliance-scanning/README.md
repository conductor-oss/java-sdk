# Scanning Infrastructure Against CIS-AWS and Auto-Remediating Findings

An audit is next month and you need to prove every resource in production complies with
your chosen framework. Manually checking 156 resources is error-prone and slow. This
workflow discovers all resources, scans them against the compliance framework, generates
an audit-ready report, and auto-remediates the critical findings.

## Workflow

```
environment, framework
         |
         v
+------------------------+     +-------------------+     +---------------------+     +----------------+
| cs_discover_resources  | --> | cs_scan_policies  | --> | cs_generate_report  | --> | cs_remediate   |
+------------------------+     +-------------------+     +---------------------+     +----------------+
  DISCOVER_RESOURCES-1480       CIS-AWS scanned           report generated            2 critical
  156 resources found           scan_policies=true        generate_report=true        findings fixed
```

## Workers

**DiscoverResourcesWorker** -- Scans the target environment and finds 156 resources.
Returns `discover_resourcesId: "DISCOVER_RESOURCES-1480"`.

**ScanPoliciesWorker** -- Scans discovered resources against the CIS-AWS framework. Returns
`scan_policies: true`.

**GenerateReportWorker** -- Compiles scan results into an audit-ready compliance report.
Returns `generate_report: true`.

**RemediateWorker** -- Auto-remediates 2 critical findings. Returns `remediate: true`.

## Tests

2 unit tests cover the compliance scanning pipeline.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
