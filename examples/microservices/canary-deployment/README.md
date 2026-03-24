# Canary Deployment with Metric-Driven Promote/Rollback Decision

Deploying a new version to 100% of traffic at once is risky. This workflow deploys a canary
instance, shifts a percentage of traffic to it, analyzes error rate (0.3%) and p99 latency
(120ms) during a monitoring window, and makes a data-driven promote-or-rollback decision
based on an error rate threshold.

## Workflow

```
serviceName, newVersion, canaryPercentage
                  |
                  v
+---------------------+     +---------------------+     +------------------------+     +-----------------------------+
| cd_deploy_canary    | --> | cd_shift_traffic    | --> | cd_analyze_metrics     | --> | cd_promote_or_rollback      |
+---------------------+     +---------------------+     +------------------------+     +-----------------------------+
  deployed: true              shifted: true              errorRate: 0.3%                action: PROMOTE or ROLLBACK
  serviceName + version       percentage applied         p99Latency: 120ms              based on threshold comparison
                                                         healthy: true
```

## Workers

**DeployCanaryWorker** -- Deploys `serviceName:newVersion` as a canary. Returns
`deployed: true`.

**ShiftTrafficWorker** -- Shifts `canaryPercentage` of traffic to the canary. Returns
`shifted: true`, `percentage` applied.

**AnalyzeMetricsWorker** -- Monitors the canary: `errorRate: 0.3`, `p99Latency: 120`,
`healthy: true`.

**PromoteOrRollbackWorker** -- Compares `errorRate` to `threshold`. Outputs
`action` (PROMOTE or ROLLBACK) with the error rate and threshold values.

## Tests

4 unit tests cover canary deploy, traffic shift, metrics analysis, and the promote/rollback
decision.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
