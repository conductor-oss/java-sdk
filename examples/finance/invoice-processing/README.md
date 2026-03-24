# Invoice Processing in Java with Conductor

Invoices arrive as PDF email attachments, portal downloads, and occasionally faxes. An AP clerk spends two hours every morning opening each one, squinting at vendor names and line items, and copy-pasting amounts into the ERP. Last month they transposed two digits on a $47,500 invoice and paid $74,500. caught six weeks later during reconciliation. Another invoice sat in a shared mailbox for three weeks because the clerk was on PTO and nobody knew it was there; the vendor cut off shipments. Right now there are 340 invoices in various stages of "processing," and nobody can tell you which ones are matched to a PO, which are approved, or which are about to miss their payment terms. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate invoice processing end-to-end, receive the document, extract fields via OCR, match against purchase orders, route for approval, and process payment, so every invoice is tracked from arrival to settlement.

## The Problem

You need to process vendor invoices from receipt to payment. An invoice document arrives, OCR extracts the key fields (vendor, amount, line items, dates), the invoice is matched against a purchase order to verify the charges, a manager approves the payment, and the payment is processed. Paying an invoice without PO matching enables unauthorized charges; skipping approval violates spending controls.

Without orchestration, you'd build a single invoice handler that receives documents, calls OCR APIs, queries the PO database, emails approvers, and triggers payment. Manually handling OCR errors on poor-quality scans, retrying failed PO lookups, and tracking approval status through email threads.

## The Solution

**You just write the invoice workers. Document receipt, OCR extraction, PO matching, approval, and payment processing. Conductor handles sequential processing, automatic retries when the OCR service returns low-confidence results, and end-to-end invoice tracking for audit.**

Each invoice concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (receive, OCR extract, PO match, approve, pay), retrying if the OCR service returns low-confidence results, tracking every invoice's full lifecycle for audit, and resuming from the last step if the process crashes.

### What You Write: Workers

Five workers handle the invoice pipeline: ReceiveInvoiceWorker captures the document, OcrExtractWorker pulls key fields, MatchPoWorker validates against the purchase order, ApproveInvoiceWorker routes for approval, and ProcessPaymentWorker triggers payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveInvoiceWorker** | `ivc_approve_invoice` | Approves the invoice |
| **MatchPoWorker** | `ivc_match_po` | Matches the po |
| **OcrExtractWorker** | `ivc_ocr_extract` | Extracting data from invoice |
| **ProcessPaymentWorker** | `ivc_process_payment` | Process Payment. Computes and returns payment status, payment id, scheduled date, payment method |
| **ReceiveInvoiceWorker** | `ivc_receive_invoice` | Receive Invoice. Computes and returns received at, document type, page count |

### The Workflow

```
ivc_receive_invoice
 │
 ▼
ivc_ocr_extract
 │
 ▼
ivc_match_po
 │
 ▼
ivc_approve_invoice
 │
 ▼
ivc_process_payment

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
