# Procurement Workflow in Java with Conductor: Requisition, Budget Approval, Purchase Order, Goods Receipt, and Payment Processing

The ops team submitted a purchase request for 50 server racks on Tuesday. It sat in a VP's inbox for nine days because she was on a trip and didn't know it was waiting for her. By the time she approved it, the supplier's bulk pricing window had closed and the unit cost went up 15%. $38,000 wasted on a signature delay. Meanwhile, accounts payable just paid an invoice for a different order where the goods never actually arrived, because there was no gate between "PO sent" and "payment released." Nobody noticed until the quarterly reconciliation, eight weeks later. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full procure-to-pay cycle, requisition, budget approval, purchase order, goods receipt, and payment, with enforced gates so nothing moves forward until the previous step is verified.

## The Problem

You need to manage procurement from request to payment. A department submits a requisition for materials with quantity and budget justification. The requisition must be approved based on budget authority and spending limits. Once approved, a purchase order is created and sent to the vendor. Goods receipt must be confirmed against the PO before payment can be released. If payment is processed before goods are verified, you risk paying for items never received or not matching specifications.

Without orchestration, requisitions arrive via email, sit in an approver's inbox for days, and purchase orders are created manually in the ERP. There is no enforced gate between goods receipt and payment. AP might pay invoices for goods that haven't arrived. When audit asks for a trail from requisition to payment for a specific purchase, reconstructing the chain across email, ERP, and the bank takes hours.

## The Solution

**You just write the procurement workers. Requisition intake, budget approval, PO creation, goods receipt, and payment processing. Conductor handles enforced approval gates, payment retries, and a complete requisition-to-payment audit trail.**

Each stage of the procure-to-pay process is a simple, independent worker, a plain Java class that does one thing. Conductor sequences them so requisitions are approved before purchase orders are created, POs are sent before goods can be received, and payment only processes after goods receipt is confirmed. If the payment processing worker fails, Conductor retries it without re-creating the purchase order. Every requisition, approval decision, PO, goods receipt, and payment is tracked with timestamps for audit and spend analytics.

### What You Write: Workers

Five workers cover the procure-to-pay cycle: RequisitionWorker creates the request, ApproveWorker validates the budget, PurchaseWorker generates the PO, ReceiveWorker confirms delivery, and PayWorker processes vendor payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `prw_approve` | Approves or rejects a requisition based on budget. |
| **PayWorker** | `prw_pay` | Processes payment for a purchase order. |
| **PurchaseWorker** | `prw_purchase` | Creates a purchase order. |
| **ReceiveWorker** | `prw_receive` | Confirms goods receipt. |
| **RequisitionWorker** | `prw_requisition` | Creates a purchase requisition. |

### The Workflow

```
prw_requisition
 │
 ▼
prw_approve
 │
 ▼
prw_purchase
 │
 ▼
prw_receive
 │
 ▼
prw_pay

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
