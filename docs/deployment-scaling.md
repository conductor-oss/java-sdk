# Deployment, scaling, and graceful shutdown

**Audience:** teams operating Java workers or agents in containers, VMs, or Spring applications.  
**Works with:** OSS and Orkes.

Scale workers from queue depth, task latency, and external dependency limits—not CPU alone. Keep each worker process stateless; Conductor persists workflow state.

## Worker lifecycle

Start polling only after configuration, credentials, and downstream dependencies are healthy. On shutdown, stop accepting new work, allow in-flight tasks to report a terminal result within the termination grace period, then call the runner's `shutdown()` method. Kubernetes readiness should represent the ability to poll and execute tasks, not merely an open HTTP port.

## Capacity controls

- Use independent deployments or task domains for workloads with different limits.
- Cap worker thread counts to protect databases and third-party APIs.
- Set server-side task rate and concurrency limits where appropriate.
- Scale from sustained queue backlog and age, then verify retries are not masking an outage.

## Agent runtime

`AgentRuntime.serve(...)` prepares local tool workers; deploy it as you would a worker process. Separate model/tool credentials by deployment and never give a broad production credential to an example tool.

Expected result: replacing one worker instance does not lose execution state; unfinished tasks are redelivered according to timeout policy.

Next: [observability](observability.md), [reliability](reliability.md), and [Spring integration](spring-boot.md).
