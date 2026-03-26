# Rolling Out a Config Change Across Services in Stages

Changing a config key across all services at once risks a fleet-wide outage if the value is
wrong. This workflow validates the config, plans a staged rollout (canary -> 25% -> 100%
at 5-minute intervals), applies it to svc-a, svc-b, and svc-c, and verifies all services
are healthy with the updated config.

## Workflow

```
configKey, configValue, targetServices
                |
                v
+----------------+     +---------------------+     +--------------------+     +--------------+
| cfg_validate   | --> | cfg_stage_rollout   | --> | cfg_apply_config   | --> | cfg_verify   |
+----------------+     +---------------------+     +--------------------+     +--------------+
  valid: true            stages: canary,             appliedServices:          verified: true
                         25%, 100%                    svc-a, svc-b, svc-c      allHealthy: true
                         interval: 5m                 version: 5
```

## Workers

**CfgValidateWorker** -- Validates `configKey = configValue`. Returns `valid: true`.

**CfgStageRolloutWorker** -- Plans the rollout: stages `["canary", "25%", "100%"]` with
`interval: "5m"`.

**CfgApplyWorker** -- Rolls out the config key across services. Returns
`appliedServices: ["svc-a", "svc-b", "svc-c"]`, `version: 5`.

**CfgVerifyWorker** -- Confirms all services are running with updated config. Returns
`verified: true`, `allHealthy: true`.

## Tests

8 unit tests cover validation, rollout planning, application, and verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
