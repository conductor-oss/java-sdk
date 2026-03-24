# Graduated Canary Release: 10% to 50% to Full Rollout

Shifting 100% of traffic to a new version at once is a gamble. This workflow deploys a
canary at 10%, monitors it (error rate 0.02%, p99 latency 48ms, 0 anomalies), increases
traffic to 50% with 5 instances, monitors again, and then does a full rollout to 100%.

## Workflow

```
appName, newVersion
       |
       v
+--------------------+     +---------------------+     +------------------------+     +---------------------+     +--------------------+
| cy_deploy_canary   | --> | cy_monitor_canary   | --> | cy_increase_traffic    | --> | cy_monitor_canary   | --> | cy_full_rollout    |
+--------------------+     +---------------------+     +------------------------+     +---------------------+     +--------------------+
  1 instance at 10%          errorRate: 0.02%            traffic -> 50%                 second monitor pass     trafficPercent: 100%
  deployedAt timestamp        p99: 48ms                  5 instances                                              complete: true
                              anomalies: 0
```

## Workers

**DeployCanaryWorker** -- Deploys `appName:newVersion` with `canaryInstances: 1` at
`trafficPercent: 10`.

**MonitorCanaryWorker** -- Monitors at the current traffic percentage. Returns
`errorRate: 0.02`, `p99Latency: "48ms"`, `anomaliesDetected: 0`, `healthy: true`.

**IncreaseTrafficWorker** -- Bumps traffic to 50% with `canaryInstances: 5`.

**FullRolloutWorker** -- Completes the rollout: `trafficPercent: 100`, `status: "success"`,
`complete: true`.

## Tests

9 unit tests cover canary deploy, monitoring, traffic increase, and full rollout.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
