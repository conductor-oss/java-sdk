# Shopping Cart in Java Using Conductor : Add Items, Calculate Total, Apply Discounts, Reserve Inventory

## Shopping Carts Are More Complex Than They Look

A customer adds 3 items to their cart with a coupon code. The system must validate each item (in stock, valid price, quantity limits), calculate the correct total (unit prices, quantity discounts, bundle pricing), apply the coupon (validate code, check eligibility, calculate discount), and reserve inventory (so the items aren't sold out before checkout).

The order matters: items must be added before totals can be calculated, totals must be calculated before discounts are applied (a percentage discount depends on the subtotal), and inventory should be reserved after discounts are confirmed (so abandoned carts with invalid coupons don't hold inventory unnecessarily). If the inventory reservation fails for one item (sold out since adding to cart), the cart total needs recalculation.

## The Solution

**You just write the item validation, total calculation, discount application, and inventory reservation logic. Conductor handles pricing retries, cart state durability, and complete cart lifecycle tracking.**

`AddItemsWorker` validates and adds items to the cart. checking availability, enforcing quantity limits, and confirming current pricing. `CalculateTotalWorker` computes the subtotal from item prices and quantities, applying any quantity-based or bundle pricing rules. `ApplyDiscountsWorker` processes coupon codes and promotional discounts, validating eligibility, calculating the discount amount, and applying it to the subtotal. `ReserveInventoryWorker` places soft holds on the cart items to prevent overselling during the checkout window. Conductor chains these four steps and records the cart state at each stage for conversion analytics.

### What You Write: Workers

Cart creation, item management, pricing, and checkout-readiness workers operate on cart state independently, keeping pricing logic separate from inventory checks.

| Worker | Task | What It Does |
|---|---|---|
| **AddItemsWorker** | `cart_add_items` | Validates items and adds them to the cart, assigning a cart ID and line item IDs |

### The Workflow

```
cart_add_items
 │
 ▼
cart_calculate_total
 │
 ▼
cart_apply_discounts
 │
 ▼
cart_reserve_inventory

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
