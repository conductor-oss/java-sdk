# Invoice Processing: OCR Extraction, Three-Way PO Matching, and Tiered Approval Routing

A vendor sends you an invoice for $8,437.50. Is that right? Your AP clerk squints at two line items -- "Cloud hosting Q1" and "Support license" -- multiplies mentally, adds 8.75% tax, and enters $8,437.50 into the ERP. But the PO says $9,200. Is the invoice under-billing (good) or is the PO wrong (bad)? This example builds the full accounts-payable pipeline using [Conductor](https://github.com/conductor-oss/conductor): receive the document, extract fields with computed line-item arithmetic, match against the purchase order within a 10% variance threshold, route through tiered approval ($10K to VP-Finance, $50K to CFO), and schedule payment on Net-15 or Net-30 terms.

## The Pipeline

```
ivc_receive_invoice  -->  ivc_ocr_extract  -->  ivc_match_po  -->  ivc_approve_invoice  -->  ivc_process_payment
```

The workflow accepts `invoiceId`, `vendorId`, and `documentUrl`. Data flows forward through task reference names: OCR outputs `amount` and `poNumber` (via `ivc_ocr_ref.output`), which feed into the PO matcher along with the original `vendorId`. The matcher's `matched` boolean and `poNumber` feed into approval. The final workflow output includes `invoiceId`, `amount`, `paymentStatus`, and `paymentId`. Timeout is set to 60 seconds.

## How Each Worker Actually Works

**ReceiveInvoiceWorker** (`ivc_receive_invoice`) -- Validates invoice and vendor ID formats. Invoice IDs must start with `INV-` or `INVOICE-`; vendor IDs must start with `VND-` or `VENDOR-`. Rejects malformed identifiers with `FAILED_WITH_TERMINAL_ERROR` and a specific message like `"Invalid invoiceId format: must start with 'INV-' or 'INVOICE-', got: GARBAGE"`.

**OcrExtractWorker** (`ivc_ocr_extract`) -- Generates deterministic line items from the invoice ID hash. Every invoice gets two line items: "Cloud hosting Q1" (quantity 1, base price $5,000 + `hash % 1000`) and "Support license" (quantity 1-3, unit price $3,000). The worker then performs full arithmetic validation:

```java
// Validate each line item: total = unitPrice * quantity
for (int i = 0; i < lineItems.size(); i++) {
    double expected = unitPrice * quantity;
    if (Math.abs(lineTotal - expected) > 0.01) {
        r.setStatus(TaskResult.Status.FAILED);
        r.setReasonForIncompletion("Line item " + (i + 1) + " arithmetic error...");
    }
}
```

Tax is computed at a fixed 8.75% rate (`taxRate = 0.0875`), rounded to the nearest cent. The grand total is validated against `subtotal + tax` with a $0.01 tolerance. PO numbers are generated as `"PO-2026-" + (1000 + hash % 9000)`. OCR confidence is reported as 0.98.

**MatchPoWorker** (`ivc_match_po`) -- Derives the PO amount deterministically from the PO number hash: `5000.0 + (poHash % 10000)`, giving a range of $5,000-$15,000. Computes the absolute variance and variance percentage: `Math.round((variance / poAmount) * 10000.0) / 100.0`. The match threshold is 10% -- if the invoice amount is within 10% of the PO amount, `matched = true`. Otherwise, the invoice proceeds but is flagged as unmatched.

**ApproveInvoiceWorker** (`ivc_approve_invoice`) -- Three-tier approval routing based on PO match status and amount:

| Condition | Approver | Decision |
|---|---|---|
| PO not matched | `SYSTEM` | REJECTED ("manual review required") |
| Amount > $50,000 | `CFO` | APPROVED |
| Amount > $10,000 | `VP-Finance` | APPROVED |
| Amount <= $10,000 | `AP-Manager` | Auto-approved |

**ProcessPaymentWorker** (`ivc_process_payment`) -- Determines payment terms based on vendor classification: vendors with `PREF` in their ID or whose `hashCode() % 3 == 0` get Net-15; everyone else gets Net-30. Payment method is `wire_transfer` for amounts over $10,000, `ach` otherwise. Schedules the payment date using `LocalDate.now().plusDays(netDays)`.

## How the Arithmetic Validation Chain Works

The key design decision is that arithmetic integrity is validated at multiple points. The OCR worker validates each line item (`unitPrice * quantity == lineTotal` within $0.01), then validates the grand total (`subtotal + tax == total` within $0.01). The integration test re-validates these relationships by reading the output:

```java
double subtotal = ((Number) ocrResult.getOutputData().get("subtotal")).doubleValue();
double tax = ((Number) ocrResult.getOutputData().get("tax")).doubleValue();
assertEquals(amount, subtotal + tax, 0.01, "total must equal subtotal + tax");
```

The PO match worker then computes its own variance against the PO amount. If the OCR extraction was wrong (transposed digits, misread decimal), the variance check catches it -- a $47,500 invoice against a $4,750 PO shows a 900% variance, well above the 10% threshold. The three-tier approval then adds a human gate for high-value invoices even when the PO matches.

## Workflow Input/Output Mapping

The `workflow.json` wires data between workers using Conductor's expression language:

| Worker Input | Source Expression |
|---|---|
| Match PO `extractedAmount` | `${ivc_ocr_ref.output.amount}` |
| Match PO `extractedPoNumber` | `${ivc_ocr_ref.output.poNumber}` |
| Approve `poMatched` | `${ivc_match_ref.output.matched}` |
| Payment `amount` | `${ivc_ocr_ref.output.amount}` |
| Payment `approved` | `${ivc_approve_ref.output.approved}` |

Notice that the payment worker receives `amount` directly from the OCR step (not from approval), ensuring the amount hasn't been modified in transit.

## Test Coverage

- **OcrExtractWorkerTest**: 7 tests -- data extraction, tax computation (`total == subtotal + tax`), line item totals match subtotal, arithmetic validation, missing/blank/malformed invoice IDs
- **MatchPoWorkerTest**: 5 tests -- variance computation, missing PO number, missing/negative/zero amounts
- **InvoiceProcessingIntegrationTest**: 4 tests -- full pipeline with arithmetic integrity verification, malformed invoice ID, malformed vendor ID, PO mismatch leading to rejection

**16 tests total** verifying the full pipeline arithmetic and every failure path.

---

> **How to run:** See [RUNNING.md](../../RUNNING.md) | **Production guidance:** See [PRODUCTION.md](PRODUCTION.md)
