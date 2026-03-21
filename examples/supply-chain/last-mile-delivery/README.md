# Last Mile Delivery in Java with Conductor : Driver Assignment, Route Optimization, Package Delivery, and Delivery Confirmation

## The Problem

You need to deliver packages to customers within promised time windows. Order ORD-2024-668 has a 2pm-4pm delivery window at 742 Evergreen Terrace, Springfield. A driver must be assigned based on availability, location, and vehicle capacity. The route must be optimized across all the driver's stops to minimize distance while honoring each customer's time window. The delivery must be executed and confirmed with proof. a signature, photo, or safe-place confirmation. If the delivery fails (customer not home, wrong address), a reattempt must be scheduled.

Without orchestration, dispatchers assign drivers manually, drivers navigate using their own judgment, and delivery confirmations come in as text messages or phone calls. If route optimization runs but the driver assignment changes afterward, the route is invalid. When a delivery fails, the dispatcher doesn't know until the driver calls in, and rescheduling happens via sticky notes. There is no end-to-end visibility into whether a package was delivered within its promised window.

## The Solution

**You just write the delivery workers. Driver assignment, route optimization, package delivery, and proof-of-delivery capture. Conductor handles driver-to-route coordination, automatic retries on upload failures, and end-to-end SLA tracking.**

Each step of the last mile process is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so driver assignment considers the route, routes are optimized for the assigned driver's full stop list, delivery is executed with the optimized route, and confirmation captures proof of delivery. If the delivery confirmation worker fails to upload the signature photo, Conductor retries without reassigning the driver. Every assignment, route plan, delivery attempt, and confirmation is recorded for SLA tracking and customer service visibility.

### What You Write: Workers

Four workers manage each delivery: AssignDriverWorker selects a driver by availability and capacity, OptimizeRouteWorker plans the most efficient stop sequence, DeliverWorker executes the drop-off, and ConfirmWorker captures proof of delivery.

| Worker | Task | What It Does |
|---|---|---|
| **AssignDriverWorker** | `lmd_assign_driver` | Assigns a driver based on availability, location, and vehicle capacity. |
| **ConfirmWorker** | `lmd_confirm` | Captures proof of delivery. signature, photo, or safe-place confirmation. |
| **DeliverWorker** | `lmd_deliver` | Executes the delivery to the customer address within the time window. |
| **OptimizeRouteWorker** | `lmd_optimize_route` | Optimizes the delivery route across all stops to minimize distance while honoring time windows. |

### The Workflow

```
lmd_assign_driver
 │
 ▼
lmd_optimize_route
 │
 ▼
lmd_deliver
 │
 ▼
lmd_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
