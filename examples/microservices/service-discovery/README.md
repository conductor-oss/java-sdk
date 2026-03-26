# Service Discovery with Least-Connections Selection and Failover

Hard-coding service URLs breaks when instances scale up or down. This workflow discovers
service instances from Consul, selects the best one using a least-connections strategy,
calls it, and handles failover if the call fails.

## Workflow

```
serviceName, request
         |
         v
+--------------------------+     +-----------------------+     +--------------------+     +------------------------+
| sd_discover_services     | --> | sd_select_instance    | --> | sd_call_service    | --> | sd_handle_failover     |
+--------------------------+     +-----------------------+     +--------------------+     +------------------------+
  instances from Consul           selected by least              host:port called          failoverTriggered if
  registrySource: "consul"        connections strategy           latency: 23ms             call failed
                                                                 success: true
```

## Workers

**DiscoverServicesWorker** -- Looks up instances for `serviceName` from Consul. Returns
`instances` list and `registrySource: "consul"`.

**SelectInstanceWorker** -- Selects the instance with the fewest `connections`. Returns
`selectedInstance` and `strategy`.

**CallServiceWorker** -- Calls the selected host:port. Returns
`response: {orderId: "ORD-555", status: "processed"}`, `latency: 23`, `success: true`.

**HandleFailoverWorker** -- If the call succeeded, returns the result. If it failed,
triggers failover. Returns `finalResult` and `failoverTriggered` flag.

## Tests

32 unit tests cover service discovery, instance selection, service calls, and failover.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
