# Provisioning Cloud Infrastructure with Plan-Validate-Apply

Spinning up a resource without validating the plan first risks policy violations -- wrong
region, oversized instance, missing tags. This workflow plans the resource, validates it
against policies, provisions it, applies configuration, and verifies health before handing
it off.

## Workflow

```
resourceType, region
         |
         v
+-----------+     +--------------+     +----------------+     +----------------+     +--------------+
| ip_plan   | --> | ip_validate  | --> | ip_provision   | --> | ip_configure   | --> | ip_verify    |
+-----------+     +--------------+     +----------------+     +----------------+     +--------------+
  type, region,     no policy           RES-500001             configured=true        verified=true
  size: "medium"    violations          status: "running"
```

## Workers

**Plan** -- Takes `resourceType` and `region` inputs. Creates a plan with `type`, `region`,
and `size: "medium"`.

**Validate** -- Checks the plan against policies. No violations found. Returns `valid: true`.

**Provision** -- Creates the resource. Returns `resourceId: "RES-500001"` and
`status: "running"`.

**Configure** -- Applies configuration to the provisioned resource using `resourceId`.
Returns `configured: true`.

**Verify** -- Confirms the resource is healthy. Returns `verified: true`.

## Tests

21 unit tests cover planning, validation, provisioning, configuration, and health checks.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
