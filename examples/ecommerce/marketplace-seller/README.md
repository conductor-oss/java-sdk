# Marketplace Seller

Marketplace seller onboarding: register, verify, list products, manage orders

**Input:** `sellerId`, `businessName`, `category` | **Timeout:** 60s

## Pipeline

```
mkt_onboard_seller
    │
mkt_verify_seller
    │
mkt_list_products
    │
mkt_manage_orders
```

## Workers

**ListProductsWorker** (`mkt_list_products`)

**ManageOrdersWorker** (`mkt_manage_orders`)

**OnboardSellerWorker** (`mkt_onboard_seller`)

**VerifySellerWorker** (`mkt_verify_seller`)

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

## Workflow Definition

**Name:** `marketplace_seller_workflow` | **Tasks:** 4 | **Workers:** 4

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
