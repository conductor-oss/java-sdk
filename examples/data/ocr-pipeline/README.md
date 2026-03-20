# OCR Pipeline in Java Using Conductor :  Image Preprocessing, Text Extraction, Validation, and Structured Output

A Java Conductor workflow example for document OCR. preprocessing a document image (deskewing, binarization, contrast enhancement), extracting raw text via OCR with a confidence score, validating the extracted text against the expected document type (invoice, receipt, form), and organizing the validated fields into structured output (invoice number, date, total, due date, payment terms). Uses [Conductor](https://github.## The Problem

A scanned invoice arrives as a JPEG. slightly rotated, low contrast, with noise from the scanner. Before you can extract structured data (invoice number, date, total amount, due date), the image needs preprocessing: deskewing to straighten the text, binarization to convert to black-and-white, and contrast enhancement so faint characters are readable. The OCR engine then produces raw text with a confidence score, but raw OCR output contains errors, a "$12,450.00" might be read as "$12,45O.OO" if the engine confuses zeros and the letter O. You need to validate the extracted text against the expected document type (an invoice should have an invoice number matching a pattern like `INV-YYYY-NNNN`, a valid date, and a dollar amount) and then organize the validated fields into structured JSON that downstream systems can consume.

Without orchestration, you'd write a single method that loads the image, preprocesses it, calls Tesseract, parses the output, and structures it all at once. If the OCR confidence is low due to poor preprocessing, you'd have no way to re-run just the preprocessing with different parameters without also re-running the OCR. There's no record of what the raw OCR output looked like before validation. Was the low confidence caused by image quality or by unusual document formatting? Adding support for a new document type (receipts, shipping labels, tax forms) means modifying tightly coupled code.

## The Solution

**You just write the image preprocessing, OCR text extraction, field validation, and structured output workers. Conductor handles strict ordering so OCR runs only on preprocessed images, retries when OCR services are temporarily unavailable, and confidence score tracking at every stage.**

Each stage of the OCR pipeline is a simple, independent worker. The preprocessor takes the raw document image and applies deskewing, binarization, and contrast enhancement tailored to the document type. The text extractor runs OCR on the preprocessed image in the specified language and returns raw text with a confidence score and character count. The validator checks the extracted text against the document type's expected patterns. Confirming that an invoice has an invoice number, date, total, and due date, and produces a validation score. The structurer organizes validated fields into typed JSON output (strings for invoice numbers, dates for due dates, doubles for totals). Conductor executes them in strict sequence, ensures OCR only runs on preprocessed images, retries if the OCR service is temporarily unavailable, and tracks confidence scores and field counts at every stage. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Four workers form the OCR pipeline: preprocessing images with deskewing and contrast enhancement, extracting raw text via OCR with confidence scoring, validating fields against document type patterns, and organizing validated data into structured JSON output.

| Worker | Task | What It Does |
|---|---|---|
| **ExtractTextWorker** | `oc_extract_text` | Extracts text from a preprocessed image using OCR. |
| **PreprocessImageWorker** | `oc_preprocess_image` | Preprocesses an image for OCR: deskew, binarize, contrast-enhance. |
| **StructureOutputWorker** | `oc_structure_output` | Organizes validated fields into a structured document. |
| **ValidateTextWorker** | `oc_validate_text` | Validates extracted OCR text and identifies structured fields. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

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
java -jar target/ocr-pipeline-1.0.0.jar
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
java -jar target/ocr-pipeline-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow ocr_pipeline \
  --version 1 \
  --input '{"imageUrl": "https://example.com", "documentType": "test-value", "language": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w ocr_pipeline -s COMPLETED -c 5
```

## How to Extend

Swap in Tesseract or Google Vision for text extraction, add document-type-specific validation rules for invoices and receipts, and the OCR pipeline workflow runs unchanged.

- **PreprocessImageWorker** → use real image processing: OpenCV for deskewing via Hough line detection, adaptive thresholding for binarization, CLAHE for contrast enhancement, and noise reduction for scanner artifacts
- **ExtractTextWorker** → call real OCR engines: Tesseract OCR (via Tess4J), Google Cloud Vision API, AWS Textract, or Azure Document Intelligence, with language model selection for multi-language documents
- **ValidateTextWorker** → implement document-type-specific validation: regex patterns for invoice numbers, date parser validation, currency amount parsing, and cross-field consistency checks (does the due date come after the invoice date?)
- **StructureOutputWorker** → map validated fields to typed schemas per document type: invoice schema (invoiceNumber, date, lineItems, total, tax, dueDate), receipt schema (merchant, date, items, subtotal, tax, total), or custom schemas defined in a configuration store

Swapping in Tesseract for text extraction or adding support for new document types requires only worker-level changes, the preprocess-extract-validate-structure pipeline stays the same.

**Add new stages** by inserting tasks in `workflow.json`, for example, a table extraction step that detects and parses line-item tables within invoices, a signature detection step that identifies signed documents, or a classification step at the beginning that auto-detects the document type before routing to type-specific extraction logic.

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
ocr-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/ocrpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── OcrPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ExtractTextWorker.java
│       ├── PreprocessImageWorker.java
│       ├── StructureOutputWorker.java
│       └── ValidateTextWorker.java
└── src/test/java/ocrpipeline/workers/
    ├── ExtractTextWorkerTest.java        # 4 tests
    ├── PreprocessImageWorkerTest.java        # 4 tests
    ├── StructureOutputWorkerTest.java        # 4 tests
    └── ValidateTextWorkerTest.java        # 5 tests
```
