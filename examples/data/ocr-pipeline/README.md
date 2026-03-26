# Ocr Pipeline

An invoice processing system receives scanned documents as image files. Each image needs preprocessing (deskew, contrast enhancement), OCR text extraction, field parsing (invoice number, date, line items, total), and validation that the extracted total matches the sum of line items.

## Pipeline

```
[oc_preprocess_image]
     |
     v
[oc_extract_text]
     |
     v
[oc_validate_text]
     |
     v
[oc_structure_output]
```

**Workflow inputs:** `imageUrl`, `documentType`, `language`

## Workers

**ExtractTextWorker** (task: `oc_extract_text`)

Extracts text from a preprocessed image using OCR.

- Writes `rawText`, `confidence`, `characterCount`

**PreprocessImageWorker** (task: `oc_preprocess_image`)

Preprocesses an image for OCR: deskew, binarize, contrast-enhance.

- Writes `processedImage`, `deskewAngle`, `enhancementsApplied`

**StructureOutputWorker** (task: `oc_structure_output`)

Organizes validated fields into a structured document.

- Writes `structured`, `fieldCount`

**ValidateTextWorker** (task: `oc_validate_text`)

Validates extracted OCR text and identifies structured fields.

- Trims whitespace
- Writes `cleanText`, `fields`, `validationScore`

---

**17 tests** | Workflow: `ocr_pipeline` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
