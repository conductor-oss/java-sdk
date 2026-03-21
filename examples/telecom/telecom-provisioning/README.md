# Telecom Provisioning in Java Using Conductor

## Why Service Provisioning Needs Orchestration

Provisioning a new telecom service requires a strict sequence where each step depends on the previous one. You create a service order with the customer's details and service type. You validate that the order is compatible with the selected plan. You configure the network equipment (switches, routers, HLR/HSS entries) for the service. You activate the configured service so the customer can start using it. Finally, you send a provisioning confirmation to the customer.

If configuration fails partway through, you need to know exactly which network elements were already configured so you can retry without creating duplicate entries. If activation succeeds but the confirmation fails, the customer has working service but no notification. Without orchestration, you'd build a monolithic provisioning script that mixes order management, network configuration, and notification logic. making it impossible to swap network vendors, test activation independently, or audit which order triggered which network changes.

## The Solution

**You just write the order creation, plan validation, network configuration, service activation, and customer notification logic. Conductor handles configuration retries, activation sequencing, and provisioning audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Order creation, validation, network configuration, activation, and confirmation workers each handle one step of turning up a new telecom service.

| Worker | Task | What It Does |
|---|---|---|
| **ActivateWorker** | `tpv_activate` | Activates the configured service on the network so the customer can start using it. |
| **ConfigureWorker** | `tpv_configure` | Configures network resources (switches, routing, subscriber profiles) for the service type. |
| **ConfirmWorker** | `tpv_confirm` | Sends a provisioning confirmation to the customer with service ID and activation details. |
| **OrderWorker** | `tpv_order` | Creates a service order with customer ID and service type, returning an order ID. |
| **ValidateWorker** | `tpv_validate` | Validates the service order against the selected plan for compatibility and eligibility. |

### The Workflow

```
tpv_order
 │
 ▼
tpv_validate
 │
 ▼
tpv_configure
 │
 ▼
tpv_activate
 │
 ▼
tpv_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
