# Blue-Green Deployment with 24-Test Smoke Suite and Monitoring

You need to deploy a new version without downtime and with confidence that the green
environment works before any customer traffic reaches it. This workflow deploys to green,
runs a 24-test smoke suite (all must pass), switches traffic, and monitors the new
deployment for error rate stability.

## Workflow

```
serviceName, newVersion, imageTag
               |
               v
+--------------------+     +----------------------+     +----------------------+     +---------------------+
| bg_deploy_green    | --> | bg_validate_green    | --> | bg_switch_traffic    | --> | bg_monitor_green    |
+--------------------+     +----------------------+     +----------------------+     +---------------------+
  deployed to green          24 tests run,               blue -> green               error rate: 0.01%
  imageTag set               24 passed                   previousActive: blue        healthy: true
```

## Workers

**DeployGreenWorker** -- Deploys `serviceName` with `imageTag` to the green environment.
Returns `deployed: true`, `environment: "green"`.

**ValidateGreenWorker** -- Runs smoke tests on green: `testsRun: 24`, `testsPassed: 24`,
`healthy: true`.

**SwitchTrafficWorker** -- Shifts traffic from blue to green. Returns `previousActive: "blue"`,
`currentActive: "green"`.

**MonitorGreenWorker** -- Monitors the green deployment. Returns `errorRate: 0.01`,
`healthy: true`.

## Tests

32 unit tests cover deployment, validation, traffic switching, and monitoring.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
