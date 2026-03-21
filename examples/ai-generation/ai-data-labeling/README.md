# AI Data Labeling in Java Using Conductor : Prepare, Parallel Labelers, Reconcile Disagreements, Export

## Quality Labels Need Multiple Annotators and Reconciliation

Training a machine learning model on poorly labeled data produces a poor model. Quality labeling uses redundancy: multiple labelers annotate the same data independently, and disagreements are resolved through reconciliation (majority vote, expert review, or confidence-weighted consensus). This catches labeler mistakes and ambiguous cases.

Parallel labeling means multiple annotators work simultaneously. cutting labeling time proportionally. But all labelers must finish before reconciliation can begin. If one labeler times out, the others' work is still valid. After reconciliation, the final labeled dataset needs export in the format required by your training pipeline (COCO JSON, Pascal VOC XML, CSV with labels).

## The Solution

**You just write the data preparation, annotation, disagreement reconciliation, and labeled dataset export logic. Conductor handles parallel labeler coordination, disagreement routing, and progress tracking across annotation tasks.**

`PrepareDataWorker` loads and preprocesses the dataset. sampling, shuffling, and formatting items for annotation. `FORK_JOIN` dispatches multiple labelers to annotate the same items independently in parallel, each labeler returns labels, confidence scores, and annotation time. After `JOIN` collects all annotations, `ReconcileWorker` resolves disagreements using majority vote or confidence-weighted consensus, flagging items where labelers strongly disagreed for expert review. `ExportWorker` formats the reconciled labels into the target format and exports the labeled dataset. Conductor tracks per-labeler accuracy and agreement rates.

### What You Write: Workers

Labeling workers run in parallel with independent annotation tasks, while a reconciliation worker resolves disagreements downstream.

| Worker | Task | What It Does |
|---|---|---|
| **ExportWorker** | `adl_export` | Exports reconciled labeled samples in COCO format to the output path for model training |
| **Labeler1Worker** | `adl_labeler_1` | Labeled 500 samples. classification labels applied |
| **Labeler2Worker** | `adl_labeler_2` | Labeled 500 samples. classification labels applied |
| **PrepareDataWorker** | `adl_prepare_data` | Prepares and preprocesses the dataset samples for labeling |
| **ReconcileWorker** | `adl_reconcile` | Inter-annotator agreement: 94%. conflicts resolved |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
adl_prepare_data
 │
 ▼
FORK_JOIN
 ├── adl_labeler_1
 └── adl_labeler_2
 │
 ▼
JOIN (wait for all branches)
adl_reconcile
 │
 ▼
adl_export

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
