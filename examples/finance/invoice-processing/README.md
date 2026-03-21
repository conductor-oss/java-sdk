# Invoice Processing in Java with Conductor

Invoices arrive as PDF email attachments, portal downloads, and occasionally faxes. An AP clerk spends two hours every morning opening each one, squinting at vendor names and line items, and copy-pasting amounts into the ERP. Last month they transposed two digits on a $47,500 invoice and paid $74,500. caught six weeks later during reconciliation. Another invoice sat in a shared mailbox for three weeks because the clerk was on PTO and nobody knew it was there; the vendor cut off shipments. Right now there are 340 invoices in various stages of "processing," and nobody can tell you which ones are matched to a PO, which are approved, or which are about to miss their payment terms. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate invoice processing end-to-end, receive the document, extract fields via OCR, match against purchase orders, route for approval, and process payment, so every invoice is tracked from arrival to settlement.

## The Problem

You need to process vendor invoices from receipt to payment. An invoice document arrives, OCR extracts the key fields (vendor, amount, line items, dates), the invoice is matched against a purchase order to verify the charges, a manager approves the payment, and the payment is processed. Paying an invoice without PO matching enables unauthorized charges; skipping approval violates spending controls.

Without orchestration, you'd build a single invoice handler that receives documents, calls OCR APIs, queries the PO database, emails approvers, and triggers payment. Manually handling OCR errors on poor-quality scans, retrying failed PO lookups, and tracking approval status through email threads.

## The Solution

**You just write the invoice workers. Document receipt, OCR extraction, PO matching, approval, and payment processing. Conductor handles sequential processing, automatic retries when the OCR service returns low-confidence results, and end-to-end invoice tracking for audit.**

Each invoice concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (receive, OCR extract, PO match, approve, pay), retrying if the OCR service returns low-confidence results, tracking every invoice's full lifecycle for audit, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers handle the invoice pipeline: ReceiveInvoiceWorker captures the document, OcrExtractWorker pulls key fields, MatchPoWorker validates against the purchase order, ApproveInvoiceWorker routes for approval, and ProcessPaymentWorker triggers payment.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveInvoiceWorker** | `ivc_approve_invoice` | Approves the invoice |
| **MatchPoWorker** | `ivc_match_po` | Matches the po |
| **OcrExtractWorker** | `ivc_ocr_extract` | Extracting data from invoice |
| **ProcessPaymentWorker** | `ivc_process_payment` | Process Payment. Computes and returns payment status, payment id, scheduled date, payment method |
| **ReceiveInvoiceWorker** | `ivc_receive_invoice` | Receive Invoice. Computes and returns received at, document type, page count |

Workers implement financial operations: risk assessment, compliance checks, settlement, with realistic outputs. Replace with real financial system integrations and the workflow, audit trail, and compliance logic stay the same.

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

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/invoice-processing-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/invoice-processing-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow invoice_processing_workflow \
  --version 1 \
  --input '{"invoiceId": "INV-2026-5500", "vendorId": "VND-330", "documentUrl": "https://docs.example.com/invoices/5500.pdf"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w invoice_processing_workflow -s COMPLETED -c 5

```

## How to Extend

Connect OcrExtractWorker to your OCR service (ABBYY, AWS Textract), MatchPoWorker to your ERP purchase order module, and ProcessPaymentWorker to your accounts payable system. The workflow definition stays exactly the same.

- **OCR extractor**: use AWS Textract, Google Document AI, or ABBYY to extract invoice fields from PDF/image documents
- **PO matcher**: query your procurement system (SAP Ariba, Coupa, Oracle Procurement Cloud) for matching purchase orders and validate line items
- **Approver**: route to the correct approver based on amount thresholds and vendor category; use a WAIT task for human approval
- **Payment processor**: trigger payment via your AP system (SAP, Oracle AP, Bill.com) with proper GL coding and payment terms

Replace simulated OCR and PO matching with real document intelligence and ERP integrations while preserving the same output fields, and the invoice pipeline requires no workflow changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
invoice-processing/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/invoiceprocessing/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── InvoiceProcessingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApproveInvoiceWorker.java
│       ├── MatchPoWorker.java
│       ├── OcrExtractWorker.java
│       ├── ProcessPaymentWorker.java
│       └── ReceiveInvoiceWorker.java
└── src/test/java/invoiceprocessing/workers/
    ├── MatchPoWorkerTest.java        # 2 tests
    └── OcrExtractWorkerTest.java        # 2 tests

```
