# Reconfiguring a Load Balancer Without Dropping Connections

Changing load balancer routing rules on a live system risks dropping connections if you
apply the config before discovering all backends. This workflow discovers 6 backend servers,
configures weighted round-robin routing rules, applies the configuration without connection
drops, and verifies all backends are healthy with traffic flowing.

## Workflow

```
loadBalancer, algorithm
          |
          v
+-----------------------+     +----------------------+     +------------------+     +------------------+
| lb_discover_backends  | --> | lb_configure_rules   | --> | lb_apply_config  | --> | lb_health_check  |
+-----------------------+     +----------------------+     +------------------+     +------------------+
  DISCOVER_BACKENDS-1354       weighted round-robin         applied without          all backends
  6 backend servers            rules updated                connection drops         healthy
```

## Workers

**DiscoverBackendsWorker** -- Takes the `loadBalancer` name and discovers 6 backend servers.
Returns `discover_backendsId: "DISCOVER_BACKENDS-1354"`.

**ConfigureRulesWorker** -- Updates routing rules to weighted round-robin. Returns
`configure_rules: true`.

**ApplyConfigWorker** -- Applies the configuration without dropping existing connections.
Returns `apply_config: true`.

**HealthCheckWorker** -- Verifies all backends are healthy and traffic is flowing. Returns
`health_check: true` with a `completedAt` timestamp.

## Tests

17 unit tests cover backend discovery, rule configuration, config application, and health
checks.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
