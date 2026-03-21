# Shipping Workflow in Java Using Conductor : Select Carrier, Create Label, Track, Deliver, Confirm

## Shipping Involves Multiple Carriers and Real-Time Tracking

A 5 lb package going from New York to Los Angeles with next-day shipping. FedEx quotes $32, UPS quotes $29, USPS doesn't offer next-day for this weight. The system must compare rates across carriers, select the cheapest qualifying option, generate a shipping label with barcode and customs information (for international), track the package through pickup, transit, and delivery, and confirm delivery with signature verification if required.

Carrier APIs are notoriously unreliable. rate requests timeout, label generation returns errors during peak season, and tracking updates arrive out of order. If label creation fails with FedEx, the system should try UPS without re-running the rate comparison. Delivery confirmation may take days, requiring the workflow to wait for carrier webhooks or polling updates.

## The Solution

**You just write the carrier selection, label creation, tracking, and delivery confirmation logic. Conductor handles carrier retries, label generation sequencing, and shipment tracking across providers.**

`SelectCarrierWorker` compares rates across carriers for the given weight, dimensions, origin, destination, and speed requirement, selecting the optimal option. `CreateLabelWorker` generates the shipping label with the selected carrier. including barcode, tracking number, and any customs documentation. `TrackWorker` monitors the package through transit milestones (picked up, in transit, out for delivery). `DeliverWorker` confirms final delivery with signature verification and proof of delivery. `ConfirmWorker` closes the shipment record and sends delivery notification to the customer. Conductor sequences these stages and records every tracking update for shipment analytics.

### What You Write: Workers

Label creation, carrier selection, tracking setup, and delivery confirmation workers each own one stage of the shipping lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **SelectCarrierWorker** | `shp_select_carrier` | Evaluates package weight, dimensions, origin/destination, and speed to pick the best carrier and service type |
| **CreateLabelWorker** | `shp_create_label` | Generates a shipping label with tracking number for the selected carrier |
| **TrackShipmentWorker** | `shp_track` | Polls the carrier for the current transit status of the shipment |
| **DeliverShipmentWorker** | `shp_deliver` | Records the delivery event at the destination address |
| **ConfirmDeliveryWorker** | `shp_confirm` | Confirms delivery back to the order system and closes out the shipment |

### The Workflow

```
shp_select_carrier
 │
 ▼
shp_create_label
 │
 ▼
shp_track
 │
 ▼
shp_deliver
 │
 ▼
shp_confirm

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
