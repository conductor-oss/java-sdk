# Marketplace Seller Onboarding in Java Using Conductor : Onboard, Verify, List Products, Manage Orders

Marketplace seller onboarding: register, verify, list products, manage orders.

## Marketplace Seller Onboarding Has Compliance Requirements

A new seller wants to sell on your marketplace. Before their products go live, you need to verify their identity (government ID, business registration), validate their business (tax ID verification, bank account confirmation), list their products with proper categorization and pricing, and enable order management (so they receive orders, manage fulfillment, and handle returns).

Each step has dependencies: products can't be listed until the seller is verified, and order management can't be enabled until products are listed. Identity verification may require manual review (flagged documents), adding variable completion time. If product listing fails (invalid images, missing required fields), the seller's verification is still valid. you just need to retry the listing step.

## The Solution

**You just write the seller registration, verification, product listing, and order management logic. Conductor handles verification retries, onboarding step sequencing, and seller lifecycle tracking.**

`OnboardSellerWorker` registers the seller with business name, category, contact information, and bank details. `VerifySellerWorker` validates the seller's identity and business legitimacy. checking government ID, tax registration, bank account ownership, and business licensing. `ListProductsWorker` processes the seller's product catalog, validating images, descriptions, pricing, and categorization, and publishing approved listings. `ManageOrdersWorker` enables the seller's order management capabilities, order notifications, fulfillment tracking, and return handling. Conductor sequences these steps and records the complete onboarding journey for compliance.

### What You Write: Workers

Seller registration, verification, product listing, and order management workers handle each onboarding phase so marketplace operations stay modular.

| Worker | Task | What It Does |
|---|---|---|
| **ListProductsWorker** | `mkt_list_products` | Listing products for store |
| **ManageOrdersWorker** | `mkt_manage_orders` | Order management activated for store |
| **OnboardSellerWorker** | `mkt_onboard_seller` | Registering seller |
| **VerifySellerWorker** | `mkt_verify_seller` | Verifying documents for seller |

### The Workflow

```
mkt_onboard_seller
 │
 ▼
mkt_verify_seller
 │
 ▼
mkt_list_products
 │
 ▼
mkt_manage_orders

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
