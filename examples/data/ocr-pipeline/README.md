# OCR Pipeline in Java Using Conductor : Image Preprocessing, Text Extraction, Validation, and Structured Output

## The Problem

A scanned invoice arrives as a JPEG. slightly rotated, low contrast, with noise from the scanner. Before you can extract structured data (invoice number, date, total amount, due date), the image needs preprocessing: deskewing to straighten the text, binarization to convert to black-and-white, and contrast enhancement so faint characters are readable. The OCR engine then produces raw text with a confidence score, but raw OCR output contains errors, a "$12,450.00" might be read as "$12,45O.OO" if the engine confuses zeros and the letter O. You need to validate the extracted text against the expected document type (an invoice should have an invoice number matching a pattern like `INV-YYYY-NNNN`, a valid date, and a dollar amount) and then organize the validated fields into structured JSON that downstream systems can consume.

Without orchestration, you'd write a single method that loads the image, preprocesses it, calls Tesseract, parses the output, and structures it all at once. If the OCR confidence is low due to poor preprocessing, you'd have no way to re-run just the preprocessing with different parameters without also re-running the OCR. There's no record of what the raw OCR output looked like before validation. Was the low confidence caused by image quality or by unusual document formatting? Adding support for a new document type (receipts, shipping labels, tax forms) means modifying tightly coupled code.

## The Solution

**You just write the image preprocessing, OCR text extraction, field validation, and structured output workers. Conductor handles strict ordering so OCR runs only on preprocessed images, retries when OCR services are temporarily unavailable, and confidence score tracking at every stage.**

Each stage of the OCR pipeline is a simple, independent worker. The preprocessor takes the raw document image and applies deskewing, binarization, and contrast enhancement tailored to the document type. The text extractor runs OCR on the preprocessed image in the specified language and returns raw text with a confidence score and character count. The validator checks the extracted text against the document type's expected patterns. Confirming that an invoice has an invoice number, date, total, and due date, and produces a validation score. The structurer organizes validated fields into typed JSON output (strings for invoice numbers, dates for due dates, doubles for totals). Conductor executes them in strict sequence, ensures OCR only runs on preprocessed images, retries if the OCR service is temporarily unavailable, and tracks confidence scores and field counts at every stage. ### What You Write: Workers

Four workers form the OCR pipeline: preprocessing images with deskewing and contrast enhancement, extracting raw text via OCR with confidence scoring, validating fields against document type patterns, and organizing validated data into structured JSON output.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractTextWorker** | `oc_extract_text` | Extracts text from a preprocessed image using OCR. |
| **PreprocessImageWorker** | `oc_preprocess_image` | Preprocesses an image for OCR: deskew, binarize, contrast-enhance. |
| **StructureOutputWorker** | `oc_structure_output` | Organizes validated fields into a structured document. |
| **ValidateTextWorker** | `oc_validate_text` | Validates extracted OCR text and identifies structured fields. |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
oc_preprocess_image
 │
 ▼
oc_extract_text
 │
 ▼
oc_validate_text
 │
 ▼
oc_structure_output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
