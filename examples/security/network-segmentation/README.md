# Implementing Network Segmentation in Java with Conductor : Zone Definition, Rule Configuration, Policy Application, and Isolation Verification

## The Problem

You need to segment your network into security zones. the DMZ for public-facing services, internal zones for application servers, and restricted zones for databases and sensitive systems. Each zone needs firewall rules defining what traffic is allowed between zones, policies must be applied to enforce the rules, and isolation must be verified to ensure no unauthorized cross-zone traffic is possible.

Without orchestration, network segmentation is configured manually in firewall consoles. Rules are added ad hoc, verification is done by security auditors on quarterly schedules, and rule drift (unauthorized changes) goes undetected. A misconfigured rule can expose your database to the internet.

## The Solution

**You just write the firewall rules and zone definitions. Conductor handles ordered deployment of zones and rules, retries on firewall API failures, and a full record of every rule applied and isolation test result.**

Each segmentation step is an independent worker. zone definition, rule configuration, policy application, and isolation verification. Conductor runs them in sequence: define zones, configure rules between them, apply the policies, then verify isolation. Every segmentation operation is tracked with zone configurations, rules applied, and verification results.

### What You Write: Workers

Four workers manage segmentation end-to-end: DefineZonesWorker establishes network boundaries, ConfigureRulesWorker sets inter-zone firewall policies, ApplyPoliciesWorker deploys security groups, and VerifyIsolationWorker confirms no unauthorized cross-zone traffic.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyPoliciesWorker** | `ns_apply_policies` | Applies security group policies across the VPC to enforce zone boundaries |
| **ConfigureRulesWorker** | `ns_configure_rules` | Configures firewall rules between zones (e.g., 28 rules between DMZ, app, data, and mgmt) |
| **DefineZonesWorker** | `ns_define_zones` | Defines network security zones (e.g., DMZ, application, data, management) |
| **VerifyIsolationWorker** | `ns_verify_isolation` | Verifies zone isolation by confirming no unauthorized cross-zone traffic exists |

the workflow logic stays the same.

### The Workflow

```
ns_define_zones
 │
 ▼
ns_configure_rules
 │
 ▼
ns_apply_policies
 │
 ▼
ns_verify_isolation

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
