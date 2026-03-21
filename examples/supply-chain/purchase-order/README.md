# Purchase Order Lifecycle in Java with Conductor : PO Creation, Approval, Vendor Transmission, Order Tracking, and Goods Receipt

## The Problem

You need to manage purchase orders from creation through delivery. A PO must be created with vendor, items, quantities, and pricing. It requires approval based on the total amount and the requester's authority level. The approved PO must be transmitted to the vendor (via EDI, email, or supplier portal). The order must be tracked for fulfillment status (acknowledged, shipped, partial delivery). Finally, goods receipt must be confirmed against the original PO line items.

Without orchestration, buyers create POs in the ERP but send them to vendors via email separately. sometimes the email fails and the vendor never receives the order. Tracking is manual: buyers call vendors weekly to check status. When goods arrive, the receiving team doesn't always know which PO to match against, leading to receiving errors and payment delays.

## The Solution

**You just write the PO lifecycle workers. Creation, approval, vendor transmission, order tracking, and goods receipt. Conductor handles transmission retries, fulfillment tracking, and full PO history for spend analytics.**

Each stage of the purchase order lifecycle is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so POs are only sent after approval, tracking begins only after the vendor confirms receipt, and goods are received only against transmitted POs. If the vendor transmission fails (EDI timeout, email bounce), Conductor retries without re-creating or re-approving the PO. Every PO creation, approval, transmission confirmation, tracking update, and receiving record is captured for spend analytics and supplier performance measurement.

### What You Write: Workers

Five workers manage PO lifecycle: CreateWorker drafts the order, ApproveWorker checks authority, SendWorker transmits to the vendor, TrackWorker monitors fulfillment, and ReceiveWorker confirms goods arrival.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `po_approve` | Approves the purchase order based on total amount and requester's authority level. |
| **CreateWorker** | `po_create` | Creates a purchase order with vendor, items, quantities, and pricing. |
| **ReceiveWorker** | `po_receive` | Confirms goods receipt against the original PO line items. |
| **SendWorker** | `po_send` | Transmits the approved PO to the vendor via EDI, email, or supplier portal. |
| **TrackWorker** | `po_track` | Tracks order fulfillment status. acknowledged, shipped, partial delivery. |

### The Workflow

```
po_create
 │
 ▼
po_approve
 │
 ▼
po_send
 │
 ▼
po_track
 │
 ▼
po_receive

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
