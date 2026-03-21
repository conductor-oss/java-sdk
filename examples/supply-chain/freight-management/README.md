# Freight Management in Java with Conductor : Carrier Booking, Shipment Tracking, Delivery Confirmation, Invoicing, and Reconciliation

## The Problem

You need to manage freight shipments end-to-end. A 2,500 lb load needs to move from Detroit, MI to Houston, TX. you book a carrier at a quoted rate, track the shipment through pickup, in-transit, and delivery milestones, confirm proof of delivery at the destination, receive the carrier's freight invoice, and reconcile the billed amount against the contracted rate (catching accessorial charges, fuel surcharges, or billing errors). If tracking data from the carrier API goes missing, you lose visibility into a $3K+ shipment.

Without orchestration, the logistics coordinator books carriers via email or phone, checks tracking portals manually, and reconciles invoices in a spreadsheet at month-end. Discrepancies between contracted rates and billed amounts go unnoticed until AP discovers them weeks later. When a shipment is delayed, there is no automated escalation. someone has to notice the tracking page hasn't updated.

## The Solution

**You just write the freight workers. Carrier booking, shipment tracking, delivery confirmation, invoicing, and rate reconciliation. Conductor handles step ordering, carrier API retries, and full shipment audit trails for freight dispute resolution.**

Each phase of the freight lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so the shipment is booked before tracking begins, delivery is confirmed before invoicing, and reconciliation catches discrepancies before payment is released. If the carrier's tracking API is temporarily unavailable, Conductor retries without re-booking the shipment. Every booking confirmation, tracking event, delivery receipt, invoice, and reconciliation result is recorded for freight audit and dispute resolution.

### What You Write: Workers

Five workers span the freight lifecycle: BookWorker reserves the carrier, TrackWorker monitors transit milestones, DeliverWorker captures proof of delivery, InvoiceWorker generates billing, and ReconcileWorker flags rate discrepancies.

| Worker | Task | What It Does |
|---|---|---|
| **BookWorker** | `frm_book` | Books the carrier with origin, destination, weight, and returns a booking ID and rate. |
| **DeliverWorker** | `frm_deliver` | Confirms delivery at the destination with proof of delivery. |
| **InvoiceWorker** | `frm_invoice` | Generates the freight invoice based on the booking rate and shipment details. |
| **ReconcileWorker** | `frm_reconcile` | Reconciles the invoiced amount against the contracted rate and flags discrepancies. |
| **TrackWorker** | `frm_track` | Tracks the shipment through pickup, in-transit, and delivery milestones. |

### The Workflow

```
frm_book
 │
 ▼
frm_track
 │
 ▼
frm_deliver
 │
 ▼
frm_invoice
 │
 ▼
frm_reconcile

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
