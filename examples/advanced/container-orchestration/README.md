# Container Orchestration

A CI/CD pipeline needs to build a Docker image, push it to a registry, deploy it to a staging environment, run integration tests, and promote to production. Each step depends on the previous one, and a failed integration test must trigger a rollback of the staging deployment.

## Pipeline

```
[ctr_build]
     |
     v
[ctr_deploy]
     |
     v
[ctr_scale]
     |
     v
[ctr_monitor]
```

**Workflow inputs:** `serviceName`, `imageTag`, `replicas`

## Workers

**CtrBuildWorker** (task: `ctr_build`)

Builds a container image for the given service and tag.

- Reads `serviceName`, `imageTag`. Writes `imageUri`, `sizeBytes`, `layers`

**CtrDeployWorker** (task: `ctr_deploy`)

Deploys a container image with the specified replica count.

- Parses strings to `int`
- Sets `status` = `"running"`
- Reads `imageUri`, `replicas`. Writes `deploymentId`, `replicas`, `status`

**CtrMonitorWorker** (task: `ctr_monitor`)

Enables monitoring for a deployment.

- Reads `serviceName`, `deploymentId`. Writes `enabled`, `dashboardUrl`

**CtrScaleWorker** (task: `ctr_scale`)

Configures auto-scaling for a deployment.

- Reads `serviceName`, `minReplicas`, `maxReplicas`. Writes `configured`, `policy`

---

**0 tests** | Workflow: `container_orchestration_demo` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
