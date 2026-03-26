# Service Registry: Register, Health Check, Discover

A new service instance starts up but nobody knows it exists until it registers, passes a
health check, and becomes discoverable by other services. Without a registry, you hard-code
URLs and deployments break whenever instances scale. This workflow registers the service at
its URL, runs a health check (12ms latency), and confirms the service is discoverable with
3 instances.

## Workflow

```
serviceName, serviceUrl, version
               |
               v
+-------------------------+     +-------------------+     +------------------------+
| sr_register_service     | --> | sr_health_check   | --> | sr_discover_service    |
+-------------------------+     +-------------------+     +------------------------+
  registrationId generated       healthy: true              endpoint:
  registered: true               latencyMs: 12              {serviceName}.internal:8080
                                                            instances: 3
```

## Workers

**RegisterServiceWorker** -- Registers `serviceName` at `serviceUrl`. Returns a
`registrationId`, `registered: true`.

**HealthCheckWorker** -- Checks `serviceUrl`. Returns `healthy: true`, `latencyMs: 12`.

**DiscoverServiceWorker** -- Discovers the service from the registry. Returns
`endpoint: "{serviceName}.internal:8080"` with `instances: 3` available for load balancing.

## Tests

23 unit tests cover registration, health checking, and service discovery.
The workflow accepts `serviceName`, `serviceUrl`, and `version` as inputs.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
