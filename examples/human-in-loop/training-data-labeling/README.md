# Training Data Labeling

Training data labeling workflow -- parallel annotators with inter-annotator agreement computation.

**Input:** `batchId` | **Timeout:** 3600s

## Pipeline

```
tdl_prepare_batch
    │
    ┌─────────────────┬─────────────────┐
    │ annotator1_wait │ annotator2_wait │
    └─────────────────┴─────────────────┘
tdl_compute_agreement
    │
tdl_store_labels
```

## Workers

**ComputeAgreementWorker** (`tdl_compute_agreement`): Worker for tdl_compute_agreement task -- compares two annotators' label arrays.

- labels1: List of label strings from annotator 1
- labels2: List of label strings from annotator 2
- agreements:    count of matching labels
- disagreements: count of differing labels

```java
int total = Math.min(labels1.size(), labels2.size());
double agreementPct = total > 0 ? (agreements * 100.0) / total : 0.0;
```

Reads `labels1`, `labels2`. Outputs `agreements`, `disagreements`, `total`, `agreementPct`.

**PrepareBatchWorker** (`tdl_prepare_batch`): Worker for tdl_prepare_batch task -- prepares a batch of data items for labeling.

Outputs `prepared`.

**StoreLabelsWorker** (`tdl_store_labels`): Worker for tdl_store_labels task -- stores the final labeled data.

Outputs `stored`.

## Tests

**15 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
