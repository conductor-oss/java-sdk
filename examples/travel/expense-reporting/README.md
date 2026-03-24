# Expense Reporting in Java with Conductor

Expense reporting: collect, categorize, submit, approve, reimburse.

## The Problem

You need to process a travel expense report from receipt collection through reimbursement. An employee returns from a trip with receipts for flights, hotels, meals, and transportation. You collect all receipts and line items. You categorize each expense (airfare, lodging, meals, ground transport, miscellaneous). You submit the categorized report for approval. The manager reviews and approves or rejects it. Finally, approved expenses are reimbursed to the employee's payroll or bank account.

If categorization misclassifies a meal as lodging, the report totals are wrong and the employee gets audited. If approval succeeds but the reimbursement payment fails, the employee is out of pocket with no visibility into when they'll be paid. Without orchestration, you'd build a monolithic expense handler that mixes OCR receipt parsing, category rules, approval workflows, and payment processing. making it impossible to update categorization rules, test approval logic independently, or audit which receipts drove which reimbursement amounts.

## The Solution

**You just write the receipt collection, expense categorization, report submission, approval, and reimbursement logic. Conductor handles receipt processing retries, validation sequencing, and expense audit trails.**

CollectWorker gathers all receipts and line items for the trip. CategorizeWorker classifies each expense into the correct category (airfare, lodging, meals, ground transport) based on vendor type and amount. SubmitWorker assembles the categorized report with totals per category and submits it for review. ApproveWorker routes the report to the employee's manager for approval. ReimburseWorker processes the approved amount through payroll or direct deposit. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Receipt capture, categorization, policy validation, and reimbursement workers each manage one step of turning travel receipts into approved expenses.

| Worker | Task | What It Does |
|---|---|---|
| **ApproveWorker** | `exr_approve` | Processing expense reporting step |
| **CategorizeWorker** | `exr_categorize` | Processing expense reporting step |
| **CollectWorker** | `exr_collect` | Processing expense reporting step |
| **ReimburseWorker** | `exr_reimburse` | Processing expense reporting step |
| **SubmitWorker** | `exr_submit` | Processing expense reporting step |

### The Workflow

```
exr_collect
 │
 ▼
exr_categorize
 │
 ▼
exr_submit
 │
 ▼
exr_approve
 │
 ▼
exr_reimburse

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
