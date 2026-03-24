# Delivery Tracking in Java with Conductor

Tracks a food delivery end-to-end: assigning a driver, recording pickup, tracking location en route, confirming delivery, and closing the order.

## The Problem

You need to track a food delivery from restaurant to customer. The workflow assigns an available delivery driver, records the pickup at the restaurant, tracks the driver's location en route, confirms delivery at the customer's address, and records completion. Late deliveries lead to cold food and unhappy customers; losing track of a driver means the customer has no ETA.

Without orchestration, you'd build a single delivery service that queries driver availability, updates status through the delivery lifecycle, sends push notifications, and handles exceptions (driver cancellation, wrong address). manually managing driver state across concurrent deliveries.

## The Solution

**You just write the driver assignment, pickup recording, location tracking, delivery confirmation, and order closure logic. Conductor handles tracking retries, ETA recalculations, and delivery chain audit trails.**

Each delivery concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (assign driver, pickup, track, deliver, confirm), retrying if the GPS tracking service is unavailable, tracking every delivery's full lifecycle, and resuming from the last step if the process crashes.

### What You Write: Workers

Pickup confirmation, route tracking, ETA updates, and delivery completion workers each own one segment of the last-mile delivery chain.

| Worker | Task | What It Does |
|---|---|---|
| **AssignDriverWorker** | `dlt_assign_driver` | Assigns an available driver to the order and returns the driver ID, name, and ETA |
| **ConfirmWorker** | `dlt_confirm` | Confirms delivery completion for the order with a final status and customer rating |
| **DeliverWorker** | `dlt_deliver` | Records that the order has been delivered by the assigned driver with a delivery timestamp |
| **PickupWorker** | `dlt_pickup` | Records the driver picking up the order at the restaurant with a pickup timestamp |
| **TrackWorker** | `dlt_track` | Tracks the driver's GPS location (lat/lng) en route to the destination and returns the current ETA |

### The Workflow

```
dlt_assign_driver
 │
 ▼
dlt_pickup
 │
 ▼
dlt_track
 │
 ▼
dlt_deliver
 │
 ▼
dlt_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
