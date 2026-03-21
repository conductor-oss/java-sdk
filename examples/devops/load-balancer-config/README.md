# Load Balancer Configuration in Java with Conductor : Discover Backends, Configure Rules, Apply, Health Check

Load balancer configuration workflow: discover backends, configure rules, apply config, and health check. ## Load Balancer Changes Need Discovery, Rules, and Verification

A new version of the API service is deployed to 3 instances. The load balancer needs to know about these instances (their IPs and ports), have the routing rules updated (path-based routing for /api/v2, header-based routing for canary traffic, weighted distribution across versions), apply the new configuration without dropping existing connections, and verify that health checks pass for all backends.

Applying a load balancer configuration change without verification is dangerous. a misconfigured rule can blackhole traffic, and a bad backend can cause cascading failures. The discovery step ensures the load balancer knows about all healthy backends. The rule configuration step defines how traffic is distributed. The apply step makes the changes live. The health check step confirms everything works end-to-end.

## The Solution

**You write the backend discovery and routing rules. Conductor handles the discover-configure-apply-verify sequence and records every configuration change.**

`DiscoverBackendsWorker` finds all available backend instances for the service. querying service discovery, Kubernetes endpoints, or cloud provider APIs for healthy instances with their connection details. `ConfigureRulesWorker` builds the routing configuration, path-based rules, host-based rules, weighted distribution, sticky sessions, and health check parameters. `ApplyConfigWorker` applies the configuration to the load balancer with graceful connection draining for removed backends. `HealthCheckWorker` verifies all backends are receiving traffic and responding correctly, checking response codes, latency, and error rates post-configuration. Conductor records every configuration change for audit.

### What You Write: Workers

Four workers configure the load balancer. Discovering backends, building routing rules, applying the config, and verifying health checks.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyConfigWorker** | `lb_apply_config` | Applies the load balancer configuration without dropping connections. |
| **ConfigureRulesWorker** | `lb_configure_rules` | Configures routing rules for the load balancer. |
| **DiscoverBackendsWorker** | `lb_discover_backends` | Discovers backend servers for the load balancer. |
| **HealthCheckWorker** | `lb_health_check` | Performs health check on all backends after config change. |

the workflow and rollback logic stay the same.

### The Workflow

```
lb_discover_backends
 │
 ▼
lb_configure_rules
 │
 ▼
lb_apply_config
 │
 ▼
lb_health_check

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
