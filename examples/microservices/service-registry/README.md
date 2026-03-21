# Service Registry in Java with Conductor

Service registry workflow that registers a service, performs a health check, and discovers the service endpoint. ## The Problem

When a new service instance starts up, it must register itself with a service registry so other services can discover it. After registration, a health check confirms the instance is ready to receive traffic, and then the registry can provide its endpoint to callers.

Without orchestration, registration and health checks are handled by client libraries (Eureka client, Consul agent) with no centralized view of the registration lifecycle. If a health check fails after registration, the instance may still receive traffic from stale discovery cache.

## The Solution

**You just write the registration, health-check, and discovery workers. Conductor handles registration sequencing, health-check retries, and a durable record of every registration lifecycle.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Three workers manage the registration lifecycle: RegisterServiceWorker adds the instance to the registry, HealthCheckWorker confirms it is ready for traffic, and DiscoverServiceWorker retrieves its endpoint for callers.

| Worker | Task | What It Does |
|---|---|---|
| **DiscoverServiceWorker** | `sr_discover_service` | Discovers a service's endpoint from the registry. |
| **HealthCheckWorker** | `sr_health_check` | Performs a health check on a registered service. |
| **RegisterServiceWorker** | `sr_register_service` | Registers a service in the service registry. |

the workflow coordination stays the same.

### The Workflow

```
sr_register_service
 │
 ▼
sr_health_check
 │
 ▼
sr_discover_service

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
