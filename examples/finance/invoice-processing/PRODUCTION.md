# Invoice Processing -- Production Deployment Guide

## Required Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `CONDUCTOR_BASE_URL` | Yes | `http://localhost:8080/api` | Conductor server API endpoint |

## Security Considerations

- **Input validation**: All workers validate required inputs and reject with `FAILED_WITH_TERMINAL_ERROR`. Invoice IDs must match `INV-*` or `INVOICE-*` format. Vendor IDs must match `VND-*` or `VENDOR-*` format.
- **Line-item arithmetic**: The OCR extract worker validates that every line item total equals unitPrice * quantity, and that the grand total equals subtotal + tax. Arithmetic errors are flagged immediately.
- **PO matching**: Invoices that do not match a purchase order within 10% variance are automatically rejected. Rejected invoices require manual review.
- **Approval thresholds**: Invoices over $50K require CFO approval; over $10K require VP-Finance. These thresholds should be externalized to configuration in production.
- **Payment methods**: Wire transfer for amounts over $10K; ACH for smaller amounts. Vendor payment terms (Net 15/30) are based on vendor status.

## Deployment Notes

- **OCR integration**: The current OCR worker generates deterministic data from invoice ID hashes. In production, integrate with a real OCR service (AWS Textract, Google Document AI, etc.) and throw `IllegalStateException` if the OCR service is unavailable.
- **PO database**: The match worker derives PO amounts from hash values. In production, connect to your ERP/procurement system for real PO lookup.
- **Vendor master data**: Vendor validation currently checks ID prefixes. In production, validate against your vendor master database.

## Monitoring Expectations

- **OCR confidence**: Monitor the `ocrConfidence` field. Values below 0.90 indicate poor document quality requiring manual data entry.
- **PO match rate**: Track the percentage of invoices that match a PO within tolerance. Low match rates indicate data entry issues or unauthorized purchases.
- **Approval throughput**: Monitor time from receipt to approval. Invoices requiring CFO approval may bottleneck -- consider delegated authority rules.
- **Payment scheduling**: Track scheduled vs actual payment dates. Missed payment deadlines damage vendor relationships.
- **Terminal errors**: `FAILED_WITH_TERMINAL_ERROR` rate should be near zero. Non-zero rates indicate malformed invoice submissions from upstream systems.
