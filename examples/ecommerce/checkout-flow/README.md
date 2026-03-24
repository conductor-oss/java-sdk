# Checkout Flow

E-commerce checkout workflow: validate cart, calculate tax, process payment, confirm order

**Input:** `cartId`, `userId`, `shippingAddress`, `paymentMethod` | **Timeout:** 60s

## Pipeline

```
chk_validate_cart
    │
chk_calculate_tax
    │
chk_process_payment
    │
chk_confirm_order
```

## Workers

**CalculateTaxWorker** (`chk_calculate_tax`): Calculates real tax, shipping, and grand total based on subtotal and shipping address.

- `FREE_SHIPPING_THRESHOLD` = `100.0`
- `DEFAULT_US_TAX_RATE` = `0.06`


Reads `shippingAddress`, `shippingMethod`, `subtotal`. Outputs `tax`, `taxRate`, `taxType`, `shipping`, `shippingMethod`.

**ConfirmOrderWorker** (`chk_confirm_order`): Confirms an order after successful payment. Performs real validations.

- Verifies payment ID is present and non-empty
- Verifies grand total is positive
- Generates a deterministic order ID from cart+user+payment data
- Records the order in an in-memory order store (thread-safe)

Reads `cartId`, `grandTotal`, `paymentId`, `userId`. Outputs `confirmed`, `error`, `orderId`, `duplicate`, `confirmedAt`.
Returns `FAILED` on validation errors.

**ProcessPaymentWorker** (`chk_process_payment`): Processes payment with real validation logic.

- Validates payment method type and required fields
- Checks for duplicate payments (idempotency via userId+amount hash)
- Applies basic card validation (Luhn check on last4 if available)
- Generates deterministic payment IDs for audit trail


Reads `amount`, `paymentMethod`, `userId`. Outputs `error`, `status`, `paymentId`, `duplicate`, `chargedAt`.
Returns `FAILED` on validation errors.

**ValidateCartWorker** (`chk_validate_cart`): Validates a shopping cart with real logic.

- Verifies each item has required fields (sku, name, price, qty)
- Checks quantities are positive and within maximum limits
- Validates prices are positive
- Calculates real subtotal from item prices * quantities

- `MAX_QUANTITY_PER_ITEM` = `100`
- `MAX_PRICE_PER_ITEM` = `50000.0`


Reads `cartId`, `couponCode`, `items`, `userId`. Outputs `valid`, `subtotal`, `itemCount`, `lineItems`, `items`.
Returns `FAILED_WITH_TERMINAL_ERROR` on invalid input.

## Tests

**44 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
