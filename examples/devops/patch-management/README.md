# Rolling Security Patches Across 24 Hosts with Staging Verification

A CVE drops and you need to patch 24 production hosts. Patching without testing first risks
breaking the application; patching all at once risks a full outage if the patch has a
regression. This workflow scans for affected hosts, tests the patch on staging (87 tests,
0 regressions), deploys in a rolling fashion, and verifies all hosts are healthy.

## Workflow

```
patchId, severity
       |
       v
+---------------------------+     +----------------+     +-------------------+     +-------------------+
| pm_scan_vulnerabilities   | --> | pm_test_patch  | --> | pm_deploy_patch   | --> | pm_verify_patch   |
+---------------------------+     +----------------+     +-------------------+     +-------------------+
  SCAN-1352                        87 tests run          24 hosts patched          all 24 hosts
  affectedHosts from severity      0 regressions        rolling deployment        verified healthy
  vulnerabilityCount = hosts*2     stagingPassed=true
```

## Workers

**ScanVulnerabilities** -- Takes `patchId` and `severity`. Computes `affectedHosts` from
the severity level and `vulnerabilityCount = affectedHosts * 2`. Returns
`scanId: "SCAN-1352"`.

**TestPatch** -- Tests the patch on staging: runs 87 tests with 0 regressions. Returns
`stagingPassed: true`, `testsRun: 87`.

**DeployPatch** -- Deploys the patch to production in a rolling fashion. Only proceeds if
`stagingPassed` is true. Returns `hostsPatched: 24`,
`deploymentStrategy: "rolling"`.

**VerifyPatch** -- Confirms all 24 patched hosts are healthy. Returns `verified: true`,
`allHealthy: true`, `hostsVerified` matching patched count, and
`completedAt: "2026-03-14T10:00:00Z"`.

## Tests

33 unit tests cover vulnerability scanning, staging tests, rolling deployment, and host
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
