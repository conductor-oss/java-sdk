# Document Verification in Java Using Conductor : AI Data Extraction, Human Verification via WAIT, and Verified Data Storage

## The Problem

You need to process documents. invoices, contracts, tax forms, identity documents, by extracting structured data from unstructured images or PDFs. AI/OCR models extract fields like names, dates, amounts, and document numbers, along with a confidence score for each extraction. But AI extraction is not perfect, handwriting misreads, low-quality scans, and unusual layouts cause errors. A human must verify the extracted data against the original document, correcting any mistakes before the data enters your system of record. Without verification, OCR errors propagate into your database, wrong amounts on invoices, misspelled names on contracts, incorrect tax IDs.

Without orchestration, you'd call the OCR API, store the raw extraction in a database, email a reviewer with a link, poll for their corrections, and then update the database with verified data. If the OCR API times out on a large batch, you'd need retry logic. If the system crashes after the reviewer corrects the data but before it is stored, the verified corrections are lost. There is no visibility into how many documents are awaiting verification, what the AI confidence scores are, or how often humans correct the AI's output.

## The Solution

**You just write the AI/OCR extraction and verified-data storage workers. Conductor handles the pause for human verification and the confidence-tracking pipeline.**

The WAIT task is the key pattern here. After the AI extracts data with confidence scores, the workflow pauses at the WAIT task. Conductor presents the extracted fields and confidence to the human verifier, who corrects any errors and submits the verified data via the API. The store worker then persists the human-verified data as the authoritative record. Conductor takes care of holding the extracted data while a reviewer verifies it, passing the reviewer's corrected data to storage, tracking extraction confidence versus human corrections (useful for retraining the AI model), and maintaining a complete audit trail from raw document through AI extraction to human-verified output.

### What You Write: Workers

AiExtractWorker runs OCR to pull structured fields with confidence scores, and StoreVerifiedWorker persists the human-corrected data, the verification step between them pauses durably for the reviewer.

| Worker | Task | What It Does |
|---|---|---|
| **AiExtractWorker** | `dv_ai_extract` | Runs AI/OCR extraction on the document, returning structured fields (name, date, amount, etc.) with a confidence score indicating extraction quality |
| *WAIT task* | `dv_human_verify` | Pauses with the extracted data and confidence score until a human reviewer verifies and corrects the fields, submitting verified data via `POST /tasks/{taskId}` | Built-in Conductor WAIT. no worker needed |
| **StoreVerifiedWorker** | `dv_store_verified` | Stores the human-verified data as the authoritative record in the document management system or database |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
dv_ai_extract
 │
 ▼
dv_human_verify [WAIT]
 │
 ▼
dv_store_verified

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
