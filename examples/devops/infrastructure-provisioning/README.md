# Infrastructure Provisioning in Java with Conductor : Plan, Validate, Provision, Configure, Verify

Orchestrates infrastructure provisioning: plan, validate, provision, configure, and verify across cloud providers.

## Infrastructure Provisioning Needs Guard Rails

An engineer requests 3 EC2 instances in us-east-1. Before spinning them up, the request needs validation: Does the account have sufficient quota? Does the instance type comply with organization policies (no m5.24xlarge without VP approval)? Is the VPC and subnet configuration correct? Will this push the monthly bill over budget?

After provisioning, the instances need configuration: install monitoring agents, configure security groups, join the service mesh, register with service discovery. If provisioning succeeds but configuration fails, you have running instances that aren't monitored or secured. a dangerous state that must be detected and remediated. Every provisioning action needs a complete audit trail for cost tracking and compliance.

## The Solution

**You write the provisioning and policy validation logic. Conductor handles the plan-validate-provision-configure-verify pipeline and the full infrastructure audit trail.**

`PlanWorker` generates the infrastructure plan. resource types, sizes, regions, networking, and estimated cost. `ValidateWorker` checks the plan against organizational policies, quota limits, budget constraints, and security requirements. `ProvisionWorker` creates the validated resources, launching instances, creating databases, provisioning load balancers. `ConfigureWorker` sets up the provisioned resources, installing agents, configuring security, joining service meshes, and setting up monitoring. `VerifyWorker` confirms all resources are operational, health checks passing, monitoring reporting, and traffic routing correctly. Conductor sequences these five steps and records the complete provisioning audit trail.

### What You Write: Workers

Five workers manage provisioning. Planning resources, validating against policies, provisioning infrastructure, configuring instances, and verifying health.

| Worker | Task | What It Does |
|---|---|---|
| **Configure** | `ip_configure` | Applies configuration to the provisioned resource. |
| **Plan** | `ip_plan` | Creates an infrastructure provisioning plan. |
| **Provision** | `ip_provision` | Provisions the infrastructure resource. |
| **Validate** | `ip_validate` | Validates the infrastructure plan against policies. |
| **Verify** | `ip_verify` | Verifies the provisioned resource is healthy. |

the workflow and rollback logic stay the same.

### The Workflow

```
ip_plan
 │
 ▼
ip_validate
 │
 ▼
ip_provision
 │
 ▼
ip_configure
 │
 ▼
ip_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
