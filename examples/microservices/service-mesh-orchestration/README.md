# Service Mesh Setup: Sidecar, mTLS, Traffic Policy, Validation

Adding a service to the mesh requires deploying an Envoy sidecar, configuring mutual TLS,
setting traffic policies (retries, timeouts), and validating connectivity. Doing these steps
out of order or skipping one leaves gaps in security or observability. This workflow
sequences all four.

## Workflow

```
serviceName, namespace, meshType
               |
               v
+-----------------------+     +----------------------+     +--------------------------+     +-----------------+
| mesh_deploy_sidecar   | --> | mesh_configure_mtls  | --> | mesh_set_traffic_policy  | --> | mesh_validate   |
+-----------------------+     +----------------------+     +--------------------------+     +-----------------+
  sidecarId:                    enabled: true               applied: true                    valid: true
  envoy-{hashCode}             certExpiry: 2027-01-01      retries: 3                       latencyMs: 5
  version: "1.28"                                           timeout: "30s"
```

## Workers

**DeploySidecarWorker** -- Deploys an Envoy proxy for `serviceName` in `namespace`. Returns
`sidecarId: "envoy-{hashCode}"`, `version: "1.28"`.

**ConfigureMtlsWorker** -- Configures mutual TLS. Returns `enabled: true`,
`certExpiry: "2027-01-01"`.

**SetTrafficPolicyWorker** -- Sets traffic policy for `meshType`. Returns `applied: true`,
`retries: 3`, `timeout: "30s"`.

**ValidateWorker** -- Validates connectivity through the mesh. Returns `valid: true`,
`latencyMs: 5`.

## Tests

30 unit tests cover sidecar deployment, mTLS configuration, traffic policies, and
connectivity validation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
