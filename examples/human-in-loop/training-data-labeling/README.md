# Training Data Labeling in Java Using Conductor : Batch Preparation, Parallel Annotator WAIT Tasks via FORK_JOIN, Inter-Annotator Agreement Computation, and Label Storage

## Training Data Labeling Needs Parallel Annotators and Agreement Computation

High-quality training data requires multiple annotators to label the same items independently (via parallel WAIT tasks), then compute inter-annotator agreement to measure label reliability. The workflow prepares a batch, two annotators label it in parallel, agreement is computed (e.g., Cohen's kappa), and the final labels are stored. If the agreement computation fails, you need to retry it without asking the annotators to re-label.

## The Solution

**You just write the batch-preparation, agreement-computation, and label-storage workers. Conductor handles the parallel annotator waits and the join.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

PrepareBatchWorker identifies items to label, ComputeAgreementWorker compares annotator labels element-by-element, and StoreLabelsWorker persists the final training data, the parallel annotator WAIT tasks are orchestrated via FORK_JOIN.

| Worker | Task | What It Does |
|---|---|---|
| **PrepareBatchWorker** | `tdl_prepare_batch` | Prepares a batch of unlabeled data items for annotation. identifies the items to label and signals the batch is ready for annotators |
| *FORK_JOIN + WAIT* | `annotator1_wait` / `annotator2_wait` | Two parallel WAIT tasks. each annotator independently labels the batch items and submits their labels array via `POST /tasks/{taskId}`; the JOIN waits for both to complete before proceeding | Built-in Conductor FORK_JOIN + WAIT, no worker needed |
| **ComputeAgreementWorker** | `tdl_compute_agreement` | Compares the two annotators' label arrays element-by-element. counts agreements, disagreements, total items, and computes agreementPct as a quality metric for the labeled batch |
| **StoreLabelsWorker** | `tdl_store_labels` | Stores the final labeled data with both annotators' labels and the agreement percentage. persists the training data for model consumption |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
tdl_prepare_batch
 │
 ▼
FORK_JOIN
 ├── annotator1_wait
 └── annotator2_wait
 │
 ▼
JOIN (wait for all branches)
tdl_compute_agreement
 │
 ▼
tdl_store_labels

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
