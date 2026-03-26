# Automating Firewall and VLAN Changes Across 12 Network Devices

Pushing firewall rules and VLAN changes to a dozen switches and routers by hand is slow and
error-prone -- one typo can partition the network. This workflow audits the current network,
plans firewall rules and VLAN changes, pushes config to all 12 devices, and verifies
connectivity end-to-end.

## Workflow

```
network, changeType
       |
       v
+--------------+     +------------------+     +------------------+     +-------------------------+
| na_audit     | --> | na_plan_changes  | --> | na_apply_config  | --> | na_verify_connectivity  |
+--------------+     +------------------+     +------------------+     +-------------------------+
  AUDIT-1353          firewall rules +         12 devices               all connectivity tests
  switches + routers  VLAN changes planned     configured               passed
```

## Workers

**AuditNetwork** -- Takes `network` and `changeType`. Discovers the device inventory
(switches and routers, deterministic from `network` name). Returns `auditId: "AUDIT-1353"`
with `totalDevices` count.

**PlanChanges** -- Plans `firewallRules` and `vlanChanges` (counts derived from audit).
Returns `planned: true`.

**ApplyConfig** -- Pushes configuration to 12 devices. Returns `applied: true`,
`devicesConfigured: 12`.

**VerifyConnectivity** -- Runs connectivity tests on all configured devices. Returns
`verified: true`, `allTestsPassed: true`, `devicesVerified` matching the configured count,
and `completedAt: "2026-03-14T10:00:00Z"`.

## Tests

32 unit tests cover network audit, change planning, config application, and connectivity
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
