# Service Discovery in Java with Conductor: Register, Health Check, Update Routing, Notify Consumers

You deployed v2.4.1 of the recommendation engine ten minutes ago. It's running, it's healthy, and it's serving exactly zero traffic. because the routing table still points to the three old instances, the load balancer doesn't know it exists, and the five downstream services that depend on it are still hitting stale endpoints. One of those old instances just ran out of memory, so now a third of your recommendation requests are 503ing and nobody can figure out why the "new deploy" didn't fix it. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to automate the full service registration lifecycle, register, health-check, update routing, and notify consumers, so new instances actually receive traffic and dead ones stop getting it.

## New Services That Are Actually Reachable

You just deployed a new instance of the user-api service. But other services cannot find it yet. It is not registered in the service mesh, the load balancer does not know about it, and downstream consumers are still routing to the old set of instances. The workflow registers the instance, verifies it passes health checks, updates routing so traffic reaches it, and notifies consumers that a new endpoint is available.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the registration and health check logic. Conductor handles the register-verify-route-notify sequence and ensures no traffic reaches an unhealthy instance.**

`RegisterWorker` adds the service instance to the discovery registry with its endpoint, version, and metadata, producing a deterministic registration ID. `HealthCheckWorker` runs initial health checks against the registered endpoint. Verifying HTTP readiness and recording response times. `UpdateRoutingWorker` configures load balancers and service meshes to route traffic to the new healthy instance. `NotifyConsumersWorker` informs downstream services of the new endpoint via webhooks, DNS updates, or service mesh configuration refresh. Conductor sequences these steps, ensuring no traffic reaches an unhealthy instance.

### What You Write: Workers

Four workers manage service registration. Registering the instance, running health probes, updating routing tables, and notifying downstream consumers.

| Worker | Task | What It Does |
|---|---|---|
| `RegisterWorker` | `sd_register` | Registers service instance in the discovery registry with a deterministic ID, endpoint, and version |
| `HealthCheckWorker` | `sd_health_check` | Probes the registered endpoint for readiness, records response time and health status |
| `UpdateRoutingWorker` | `sd_update_routing` | Updates load balancer and routing tables to include the new healthy instance |
| `NotifyConsumersWorker` | `sd_notify_consumers` | Notifies downstream consumers via webhook that a new service endpoint is available |

### The Workflow

```
sd_register
 │
 ▼
sd_health_check
 │
 ▼
sd_update_routing
 │
 ▼
sd_notify_consumers

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
