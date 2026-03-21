# Checkout Flow in Java Using Conductor: Validate Cart, Calculate Tax, Process Payment, Confirm Order

Your checkout abandonment rate is 68%. Not because customers changed their minds. because your checkout calls five APIs sequentially: inventory validation, price confirmation, tax calculation, payment processing, and order creation. Any one of them timing out kills the entire flow, and the customer sees a spinner for 12 seconds before a generic error page. They refresh, get a new cart with different prices, and leave. The tax service went down for 90 seconds during a Tuesday sale and you lost $40,000 in completed carts that never converted. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate checkout steps as independent workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Checkout Must Be Reliable and Atomic

A customer clicks "Place Order" with a $230 cart. The system must verify every item is still in stock and priced correctly (prices may have changed since the item was added), calculate the correct tax for the shipping destination (sales tax varies by state, county, and city), charge the payment method, and create the order, all as a reliable sequence where a failure at any step doesn't leave the system in an inconsistent state.

If the payment processor times out after the tax calculation, you need to retry the payment. . Not recalculate the tax. If the payment succeeds but order confirmation fails, the payment must be recorded so the customer isn't charged again on retry. Every checkout needs a complete audit trail: what was in the cart, what tax was calculated, whether payment succeeded, and the final order confirmation.

## The Solution

**You just write the cart validation, tax, payment, and order confirmation logic. Conductor handles payment retries with idempotency, step sequencing, and full checkout audit trails.**

`ValidateCartWorker` verifies each item's availability, current price, and quantity limits, returning the validated cart with the confirmed subtotal. `CalculateTaxWorker` determines the applicable tax rates based on the shipping address jurisdiction and computes the tax amount. `ProcessPaymentWorker` charges the customer's payment method for the total (subtotal + tax + shipping) and returns the transaction ID. `ConfirmOrderWorker` creates the order record with an order number, itemized receipt, and estimated delivery date. Conductor chains these four steps, retries failed payment processing with idempotency keys, and records the complete checkout for accounting.

### What You Write: Workers

Four checkout workers: cart validation, tax calculation, payment processing, and order confirmation, each encapsulate one transactional step of the purchase.

| Worker | Task | What It Does |
|---|---|---|
| **CalculateTaxWorker** | `chk_calculate_tax` | Calculates tax, shipping, and grand total based on subtotal and shipping address. |
| **ConfirmOrderWorker** | `chk_confirm_order` | Confirms the order and generates an order ID. |
| **ProcessPaymentWorker** | `chk_process_payment` | Processes payment and returns a payment ID. |
| **ValidateCartWorker** | `chk_validate_cart` | Validates the shopping cart and returns subtotal information. |

### The Workflow

```
chk_validate_cart
 │
 ▼
chk_calculate_tax
 │
 ▼
chk_process_payment
 │
 ▼
chk_confirm_order

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
