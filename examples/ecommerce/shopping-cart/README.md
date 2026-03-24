# Shopping Cart

Shopping cart workflow: add items, calculate total, apply discounts, reserve inventory

**Input:** `userId`, `items`, `couponCode` | **Timeout:** 60s

## Pipeline

```
cart_add_items
    │
cart_calculate_total
    │
cart_apply_discounts
    │
cart_reserve_inventory
```

## Workers

**AddItemsWorker** (`cart_add_items`): Adds items to a shopping cart and assigns line IDs.

```java
for (int i = 0; i < items.size(); i++) {
```

Reads `items`, `userId`. Outputs `cartId`, `cartItems`, `itemCount`.

**ApplyDiscountsWorker** (`cart_apply_discounts`): Applies discount/coupon codes to the cart subtotal.

```java
discountAmount = Math.round(subtotal * 0.10 * 100.0) / 100.0;
discountAmount = Math.min(20.0, subtotal);
```

Reads `cartId`, `couponCode`, `subtotal`. Outputs `finalTotal`, `discountApplied`, `discountAmount`, `couponCode`.

**CalculateTotalWorker** (`cart_calculate_total`): Calculates the subtotal for items in the cart.

```java
subtotal += price * qty;
double taxAmount = Math.round(subtotal * taxRate * 100.0) / 100.0;
```

Reads `cartId`, `items`. Outputs `subtotal`, `taxAmount`, `itemCount`.

**ReserveInventoryWorker** (`cart_reserve_inventory`): Reserves inventory for all items in the cart.

Reads `cartId`, `items`. Outputs `allReserved`, `reservations`, `reservationExpiry`.

## Tests

**0 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
