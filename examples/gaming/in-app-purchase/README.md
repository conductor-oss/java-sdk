# In App Purchase in Java Using Conductor

Processes an in-app purchase: selecting an item from the game catalog, verifying eligibility, charging payment, delivering the virtual item to the player's inventory, and generating a receipt. ## The Problem

You need to process an in-app purchase in a game. The player selects an item to buy, the purchase is verified with the platform's payment system (ensuring the transaction is legitimate), payment is charged, the virtual item is delivered to the player's inventory, and a receipt is generated. Delivering items without payment verification enables fraud; failing to deliver after payment creates support tickets and chargebacks.

Without orchestration, you'd handle the purchase flow in a single service that validates the item, calls the payment API, grants the item, and generates receipts. manually handling race conditions when a player buys the same item twice, retrying failed item deliveries, and reconciling payment records with inventory grants.

## The Solution

**You just write the item selection, eligibility check, payment processing, item delivery, and receipt generation logic. Conductor handles payment retries, entitlement delivery, and purchase audit trails for every transaction.**

Each purchase concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (select, verify, charge, deliver, receipt), retrying if the payment platform is temporarily unavailable, tracking every purchase with full audit trail, and resuming from the last step if the process crashes. ### What You Write: Workers

Product validation, payment processing, entitlement delivery, and receipt generation workers isolate each phase of a microtransaction.

| Worker | Task | What It Does |
|---|---|---|
| **ChargeWorker** | `iap_charge` | Charges the item price to the player's account and returns a transaction ID |
| **DeliverWorker** | `iap_deliver` | Delivers the purchased item to the player's inventory and confirms the update |
| **ReceiptWorker** | `iap_receipt` | Generates a purchase receipt with transaction ID, player ID, and completion status |
| **SelectItemWorker** | `iap_select_item` | Validates the selected item in the game catalog and returns item details (name, type, e.g., Dragon Armor cosmetic) |
| **VerifyWorker** | `iap_verify` | Verifies purchase eligibility, checking account balance and age restrictions |

### The Workflow

```
iap_select_item
 │
 ▼
iap_verify
 │
 ▼
iap_charge
 │
 ▼
iap_deliver
 │
 ▼
iap_receipt

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
