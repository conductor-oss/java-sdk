# Registering a Service with DNS Resolution and Consumer Notification

When a new service instance comes up, it needs to register, pass a health check, get added
to routing, and notify downstream consumers -- all before traffic arrives. This workflow
registers the service with real DNS resolution via `InetAddress.getAllByName`, health-checks
the endpoint, updates routing rules (writing to a local file), and notifies consumers with
their resolved addresses.

## Workflow

```
serviceName, endpoint, version, healthCheckUrl
                   |
                   v
+----------------+     +-------------------+     +----------------------+     +------------------------+
| sd_register    | --> | sd_health_check   | --> | sd_update_routing    | --> | sd_notify_consumers    |
+----------------+     +-------------------+     +----------------------+     +------------------------+
  registrationId        DNS resolution of         routing file written        consumer DNS resolved
  + DNS resolved        health endpoint           active instances counted    notification sent
  addresses
```

## Workers

**RegisterWorker** -- Registers `serviceName` at `endpoint` with `version`. Performs real
DNS resolution via `InetAddress.getAllByName` on the endpoint hostname. Returns
`registrationId`, `resolvedAddresses`, and `dnsResolved` flag.

**HealthCheckWorker** -- Resolves the health check endpoint's DNS and validates the service.
Returns the `endpoint`, `serviceName`, `version`, and `resolvedAddress`.

**UpdateRoutingWorker** -- Updates routing for the service: resolves the endpoint address,
checks port reachability, counts active instances, and writes a routing file to disk.
Returns `updated: true`.

**NotifyConsumersWorker** -- Notifies downstream consumers about the service availability
update. Resolves each consumer's DNS and marks them as notified.

## Tests

30 unit tests cover registration, health checks, routing updates, and consumer notification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
