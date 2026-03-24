# Service Discovery in Java with Conductor

Discover services, select instance, call with failover.

## The Problem

In a dynamic microservice environment, service instances come and go. Calling a service requires discovering available instances from a registry, selecting the best one (e.g., least connections, healthiest), making the call, and handling failover if the selected instance is down.

Without orchestration, service discovery is embedded in each client using libraries like Ribbon or Eureka client, with retry/failover logic duplicated across every calling service. There is no centralized view of which instance was selected, why, and what happened when it failed.

## The Solution

**You just write the discovery, instance-selection, service-call, and failover workers. Conductor handles discovery-to-call sequencing, per-call timeout enforcement, and a full record of instance selection decisions.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers handle dynamic service resolution: DiscoverServicesWorker queries the registry, SelectInstanceWorker picks the best candidate, CallServiceWorker makes the request, and HandleFailoverWorker retries on a different instance if needed.

| Worker | Task | What It Does |
|---|---|---|
| **CallServiceWorker** | `sd_call_service` | Calls the selected service instance. |
| **DiscoverServicesWorker** | `sd_discover_services` | Discovers service instances from a registry. |
| **HandleFailoverWorker** | `sd_handle_failover` | Handles failover if the service call failed. |
| **SelectInstanceWorker** | `sd_select_instance` | Selects the best service instance based on strategy (least-connections). |

the workflow coordination stays the same.

### The Workflow

```
sd_discover_services
 │
 ▼
sd_select_instance
 │
 ▼
sd_call_service
 │
 ▼
sd_handle_failover

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
