# Payment Processing

Payment processing workflow: validate, authorize, capture, receipt, reconcile

**Input:** `orderId`, `amount`, `currency`, `paymentMethod`, `merchantId` | **Timeout:** 60s

## Pipeline

```
pay_validate
    │
pay_authorize
    │
pay_capture
    │
pay_receipt
    │
pay_reconcile
```

## Workers

**AuthorizePaymentWorker** (`pay_authorize`): Creates a Stripe PaymentIntent with manual capture (authorize only, capture later).


Reads `amount`, `currency`, `orderId`. Outputs `authorizationId`, `authorized`, `stripeStatus`, `amountInCents`, `currency`.
Returns `FAILED` on validation errors.

**CapturePaymentWorker** (`pay_capture`): Captures a previously authorized Stripe PaymentIntent.


Reads `amount`, `authorizationId`. Outputs `captureId`, `captured`, `stripeStatus`, `capturedAt`, `amountCaptured`.
Returns `FAILED` on validation errors.

**ReceiptWorker** (`pay_receipt`): Generates a payment receipt after successful capture.


Reads `amount`, `captureId`, `currency`, `orderId`. Outputs `receiptId`, `orderId`, `captureId`, `amount`, `currency`.

**ReconcileWorker** (`pay_reconcile`): Reconciles a captured payment by verifying it against Stripe records.

- Retrieves the PaymentIntent from Stripe and verifies status is "succeeded"
- Verifies the captured amount matches the expected amount

- `STRIPE_PERCENTAGE_FEE` = `0.029`
- `STRIPE_FIXED_FEE` = `0.30`


Reads `amount`, `captureId`, `merchantId`. Outputs `reconciled`, `stripeStatus`, `expectedAmount`, `actualAmount`, `amountMatches`.
Returns `FAILED` on validation errors.

**ValidatePaymentWorker** (`pay_validate`): Validates a payment request before authorization.

- Checks that amount is positive and within Stripe limits (max $999,999.99)
- Validates currency is a supported ISO 4217 code

- `amount <= 0` &rarr; `"Amount must be positive"`


Reads `amount`, `currency`, `orderId`, `paymentMethod`. Outputs `valid`, `fraudScore`, `amountValid`, `currencyValid`, `paymentMethodValid`.
Returns `FAILED_WITH_TERMINAL_ERROR` on invalid input.

## Tests

**30 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
