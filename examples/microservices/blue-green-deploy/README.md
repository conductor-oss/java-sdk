# Blue-Green Deployment with Real HTTP Health Checks

Deploying a new version directly to production means any failure is immediately customer-
facing. This workflow deploys to the green environment (4 containers), runs health checks
(real HTTP requests with response time validation), switches traffic from blue to green via
DNS, and verifies the deployment is stable (0.01% error rate, 52ms p99 latency).

## Workflow

```
appName, newVersion, currentEnv
              |
              v
+---------------------+     +-------------------+     +----------------------+     +---------------------------+
| bg_prepare_green    | --> | bg_test_green     | --> | bg_switch_traffic    | --> | bg_verify_deployment      |
+---------------------+     +-------------------+     +----------------------+     +---------------------------+
  4 containers deployed      real HTTP health          activeEnv: green             errorRate: 0.01%
  imageTag set               checks + response         dnsUpdated: true             p99Latency: 52ms
  readyAt timestamp          time validation                                        rollbackAvailable: true
```

## Workers

**PrepareGreenWorker** -- Deploys `appName:newVersion` to green with `containersDeployed: 4`.

**TestGreenWorker** -- Runs real HTTP health checks via `HttpClient`, validates response
time, checks the active environment matches `"green"`, and verifies the version. Outputs
`testsPassed` and `testsFailed` counts.

**SwitchTrafficWorker** -- Switches traffic from `currentEnv` to green. Sets
`dnsUpdated: true`.

**VerifyDeploymentWorker** -- Confirms the deployment: `errorRate: "0.01%"`,
`p99Latency: "52ms"`, `rollbackAvailable: true`.

## Tests

8 unit tests cover green preparation, health check scenarios, traffic switching, and
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
