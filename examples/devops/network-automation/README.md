# Network Automation in Java with Conductor : Plan Changes, Apply Config, Audit, Verify Connectivity

Automates network infrastructure changes using [Conductor](https://github.com/conductor-oss/conductor). This workflow audits the current network state (devices, configurations, topology), plans the required configuration changes, applies them to network devices, and verifies connectivity is intact after the changes.

## Network Changes Without Outages

You need to update firewall rules across 12 switches to allow traffic from a new subnet. Doing this manually means SSHing into each device, running show commands to understand the current state, typing configuration commands, and hoping you do not fat-finger a rule that blocks production traffic. The safe approach: audit all devices first to understand the current configuration, plan the exact changes needed, apply them systematically, and verify connectivity end-to-end before declaring success.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the device configuration and verification logic. Conductor handles audit-plan-apply-verify sequencing and records every network change for compliance.**

`PlanChangesWorker` determines which devices need configuration changes and generates the specific commands for each device type (Cisco IOS, Juniper JunOS, Palo Alto). `ApplyConfigWorker` pushes the configuration changes to each network device using programmatic access. SSH, NETCONF, or vendor APIs. `AuditNetworkWorker` compares the actual device configurations against the desired state, identifying any deviations or incomplete changes. `VerifyConnectivityWorker` tests end-to-end connectivity through the network. pinging through firewalls, testing port access, and validating routing. Conductor sequences these steps and records every configuration change for network audit.

### What You Write: Workers

Four workers automate network changes. Planning configurations, applying to devices, auditing the network state, and verifying end-to-end connectivity.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyConfig** | `na_apply_config` | Applies planned configuration to network devices. |
| **AuditNetwork** | `na_audit` | Audits network infrastructure to discover devices and current configuration. |
| **PlanChanges** | `na_plan_changes` | Plans network configuration changes based on the audit. |
| **VerifyConnectivity** | `na_verify_connectivity` | Verifies network connectivity after configuration changes. |

the workflow and rollback logic stay the same.

### The Workflow

```
Input -> ApplyConfig -> AuditNetwork -> PlanChanges -> VerifyConnectivity -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
