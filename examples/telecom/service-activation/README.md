# Service Activation in Java Using Conductor

## Why Service Activation Needs Orchestration

Activating a new telecom service requires a strict sequence where each step must succeed before proceeding. You validate the service order. confirming the order exists, the customer's account is in good standing, and the requested service type is available at their location. You provision the network resources, creating subscriber profiles in the HLR/HSS, configuring access ports, and allocating bandwidth. You test the provisioned service by running automated checks to confirm connectivity and quality. You activate the service so the customer can start using it. Finally, you notify the customer with their service ID and activation details.

If provisioning succeeds but the test reveals a quality issue, you need the provisioned service ID to troubleshoot without re-provisioning. If activation succeeds but notification fails, the customer has working service but no confirmation. Without orchestration, you'd build a monolithic activation script that mixes order validation, network provisioning, test automation, and CRM notification. making it impossible to swap provisioning platforms, test service quality independently, or audit which order triggered which network changes.

## The Solution

**You just write the order validation, network provisioning, service testing, activation, and customer notification logic. Conductor handles resource allocation retries, configuration deployment, and activation audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Order validation, resource allocation, configuration deployment, and service verification workers each handle one step of activating customer services.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `sac_activate` | Activates the provisioned and tested service on the network so the customer can start using it. |
| **NotifyWorker** | `sac_notify` | Sends activation confirmation to the customer with service ID and connection details. |
| **ProvisionWorker** | `sac_provision` | Provisions network resources for the service type. subscriber profiles, access ports, bandwidth allocation. |
| **ValidateOrderWorker** | `sac_validate_order` | Validates the service order against the customer's account. checking order existence, account status, and availability. |

### The Workflow

```
sac_validate_order
 │
 ▼
sac_provision
 │
 ▼
sac_test
 │
 ▼
sac_activate
 │
 ▼
sac_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
