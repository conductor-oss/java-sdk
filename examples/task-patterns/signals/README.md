# Signals in Java with Conductor

Signals demo. send data to running workflows via WAIT task completion. Two WAIT tasks pause the workflow until external signals arrive with shipping and delivery data.

## The Problem

You need an order fulfillment workflow that pauses and waits for external events at two points: first, it waits for a shipping confirmation (tracking number and carrier) from the warehouse or shipping partner, and second, it waits for a delivery confirmation (delivery timestamp and recipient signature) from the carrier. The workflow cannot continue past each wait point until the external system sends the signal with the required data. Between signals, the order sits in a known state. prepared, shipped, or delivered, potentially for hours or days.

Without orchestration, you'd poll a database or message queue for shipping and delivery updates, maintaining a state machine in application code that tracks where each order is in the fulfillment process. If the polling service crashes, orders are stuck until it restarts. There is no built-in way for an external system to "push" data into a running process at a specific step, and correlating incoming signals with the right order workflow requires custom routing logic.

## The Solution

**You just write the order preparation, shipping processing, and delivery completion workers. Conductor handles the durable WAIT pauses and signal-to-workflow routing.**

This example demonstrates Conductor's WAIT task for pausing a workflow until an external signal arrives. SigPrepareWorker prepares the order for shipping. The workflow then hits a WAIT task (`wait_shipping`) and pauses indefinitely until an external system completes the task with shipping data (tracking number and carrier). When the signal arrives, SigProcessShippingWorker processes the shipping information. The workflow pauses again at a second WAIT task (`wait_delivery`) until the carrier sends delivery confirmation (timestamp and signature). Finally, SigCompleteWorker marks the order as delivered. Each WAIT task's output data comes from the external signal, wired into the next worker via `${wait_shipping_ref.output.trackingNumber}` and `${wait_delivery_ref.output.signature}`.

### What You Write: Workers

Three workers handle the order fulfillment flow between WAIT pauses: SigPrepareWorker readies the order for shipping, SigProcessShippingWorker handles the tracking data after the first external signal, and SigCompleteWorker marks delivery after the second signal arrives.

| Worker | Task | What It Does |
|---|---|---|
| **SigCompleteWorker** | `sig_complete` | Completes the order after the delivery signal is received. Takes orderId, deliveredAt, and signature. Returns { done:... |
| **SigPrepareWorker** | `sig_prepare` | Prepares the order for shipping. Takes an orderId and returns { ready: true }. |
| **SigProcessShippingWorker** | `sig_process_shipping` | Processes shipping information after the shipping signal is received. Takes orderId, trackingNumber, and carrier. Ret... |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
sig_prepare
 │
 ▼
wait_shipping [WAIT]
 │
 ▼
sig_process_shipping
 │
 ▼
wait_delivery [WAIT]
 │
 ▼
sig_complete

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
