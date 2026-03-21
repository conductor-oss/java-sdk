# Container Deployment Pipeline in Java Using Conductor : Build, Deploy, Scale, Monitor

## Deploying Containers End-to-End

Shipping a new version of a containerized service means building the image, pushing it to a registry, creating a deployment with the right replica count, configuring the horizontal pod autoscaler so it can scale between 2 and 10 replicas based on load, and wiring up monitoring dashboards so you can see if the new version is healthy. Each step depends on the previous one. you can't deploy an image that hasn't been built, you can't configure scaling for a deployment that doesn't exist, and monitoring is useless if it's pointed at the wrong deployment ID.

When the build fails halfway through or the deploy times out, you need to know exactly which step broke, what the image URI was, and whether the deployment was partially created. A shell script won't give you that visibility, and a CI/CD pipeline only helps if you can trace the full execution history.

## The Solution

**You write the build, deploy, and scaling logic. Conductor handles sequencing, retries, and deployment audit trails.**

`CtrBuildWorker` builds the container image and returns the image URI (e.g., `registry/service:tag`). `CtrDeployWorker` takes that URI and the desired replica count, creates the deployment, and returns a deployment ID. `CtrScaleWorker` configures auto-scaling on the deployment. setting min replicas to 2 and max to 10. `CtrMonitorWorker` enables health checks and metrics collection for the deployment. Conductor chains these steps so each one uses the output of the previous, retries any failed step, and gives you a complete audit trail of every deployment: which image, how many replicas, what scaling rules, and when monitoring was enabled.

### What You Write: Workers

Four workers span the deployment pipeline: image building, container deployment, auto-scaling configuration, and monitoring enablement, each owning one phase of the release.

| Worker | Task | What It Does |
|---|---|---|
| **CtrBuildWorker** | `ctr_build` | Builds a container image for the given service and tag. |
| **CtrDeployWorker** | `ctr_deploy` | Deploys a container image with the specified replica count. |
| **CtrMonitorWorker** | `ctr_monitor` | Enables monitoring for a deployment. |
| **CtrScaleWorker** | `ctr_scale` | Configures auto-scaling for a deployment.### The Workflow

```
ctr_build
 │
 ▼
ctr_deploy
 │
 ▼
ctr_scale
 │
 ▼
ctr_monitor

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
