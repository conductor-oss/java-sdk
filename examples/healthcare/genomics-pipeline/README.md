# Genomics Pipeline

Genomics Pipeline: sequence, align, call variants, annotate, report

**Input:** `sampleId`, `patientId`, `panelType` | **Timeout:** 60s

## Pipeline

```
gen_sequence
    │
gen_align
    │
gen_call_variants
    │
gen_annotate
    │
gen_report
```

## Workers

**AlignWorker** (`gen_align`)

Reads `reads`, `referenceGenome`. Outputs `alignmentFile`, `mappedReads`, `mappingRate`.

**AnnotateWorker** (`gen_annotate`)

Reads `variants`. Outputs `annotatedVariants`.

**CallVariantsWorker** (`gen_call_variants`)

Reads `alignmentFile`. Outputs `variants`, `totalVariants`, `snvCount`, `indelCount`.

**GenomicsReportWorker** (`gen_report`)

```java
long significant = annotated.stream()
```

Reads `annotatedVariants`. Outputs `reportId`, `clinicallySignificant`, `actionableFindings`, `generatedAt`.

**SequenceWorker** (`gen_sequence`)

Reads `panelType`, `sampleId`. Outputs `totalReads`, `meanQuality`, `referenceGenome`, `coverageDepth`.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
