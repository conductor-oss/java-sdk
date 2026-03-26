# Deploying Across Three Regions with Sequential Verification

Pushing a new version to all regions at once means a bad deploy takes down your entire
global footprint. This workflow deploys to the primary region first (us-east-1), verifies it
is healthy, then rolls out to two secondary regions (eu-west-1, ap-southeast-1), and runs a
global health check across all three.

## Workflow

```
service, version, regions
          |
          v
+---------------------+     +---------------------+     +-----------------------+     +---------------------+
| mrd_deploy_primary  | --> | mrd_verify_primary  | --> | mrd_deploy_secondary  | --> | mrd_verify_global   |
+---------------------+     +---------------------+     +-----------------------+     +---------------------+
  DEPLOY_PRIMARY-1362        us-east-1 healthy           eu-west-1 +                   all 3 regions
  us-east-1                  proceed to secondary        ap-southeast-1                healthy & serving
```

## Workers

**DeployPrimaryWorker** -- Deploys `service` at `version` to us-east-1. Returns
`deploy_primaryId: "DEPLOY_PRIMARY-1362"` with `region: "us-east-1"`.

**VerifyPrimaryWorker** -- Confirms us-east-1 is healthy. Returns `verify_primary: true`.

**DeploySecondaryWorker** -- Deploys to eu-west-1 and ap-southeast-1. Returns
`deploy_secondary: true` with `regions: "eu-west-1, ap-southeast-1"`.

**VerifyGlobalWorker** -- Confirms all 3 regions are healthy and serving traffic. Returns
`verify_global: true` and `completedAt: "2026-03-14T00:00:00Z"`.

## Tests

30 unit tests cover primary deploy, primary verification, secondary deploy, and global
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
