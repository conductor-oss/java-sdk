# Service Mesh Orchestration in Java with Conductor

Orchestrates service mesh configuration: deploy sidecar proxies, configure mTLS, set traffic policies, and validate connectivity. ## The Problem

Onboarding a service into a service mesh requires deploying a sidecar proxy (e.g., Envoy), configuring mutual TLS for encrypted service-to-service communication, setting traffic policies (retries, timeouts, circuit breaking), and validating end-to-end connectivity. Each step depends on the previous one. MTLS cannot be configured until the sidecar is deployed.

Without orchestration, mesh setup is a series of kubectl apply commands in a runbook. If the mTLS step fails, the traffic policy may still be applied, leaving the service in a half-configured state. There is no audit trail of which services are fully mesh-enabled.

## The Solution

**You just write the sidecar-deploy, mTLS-config, traffic-policy, and validation workers. Conductor handles ordered mesh setup, crash-safe resume between sidecar deploy and mTLS configuration, and a full audit of mesh enrollment.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

Four workers onboard a service into the mesh: DeploySidecarWorker injects the proxy, ConfigureMtlsWorker sets up mutual TLS, SetTrafficPolicyWorker applies retry and timeout rules, and ValidateWorker confirms end-to-end connectivity.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureMtlsWorker** | `mesh_configure_mtls` | Configures mutual TLS for a service. |
| **DeploySidecarWorker** | `mesh_deploy_sidecar` | Deploys a sidecar proxy for a service in a given namespace. |
| **SetTrafficPolicyWorker** | `mesh_set_traffic_policy` | Sets traffic policy for a service mesh. |
| **ValidateWorker** | `mesh_validate` | Validates connectivity after mesh configuration. |

the workflow coordination stays the same.

### The Workflow

```
mesh_deploy_sidecar
 │
 ▼
mesh_configure_mtls
 │
 ▼
mesh_set_traffic_policy
 │
 ▼
mesh_validate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
