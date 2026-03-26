# Human Task

Human task demo -- collect_data, WAIT (demonstrating HUMAN task with form schema), process_form.

**Input:** `applicantName` | **Timeout:** 3600s

**Output:** `collected`, `decision`

## Pipeline

```
ht_collect_data
    │
human_review_wait [WAIT]
    │
ht_process_form
```

## Workers

**CollectDataWorker** (`ht_collect_data`): Worker for ht_collect_data — collects initial data before the human review step.

Outputs `collected`.

**ProcessFormWorker** (`ht_process_form`): Worker for ht_process_form — processes the human review decision.

```java
String decision = approved ? "application-approved" : "application-rejected";
```

Reads `approved`. Outputs `decision`.

## Workflow Output

- `collected`: `${collect_data_ref.output.collected}`
- `decision`: `${process_form_ref.output.decision}`

## Data Flow

**ht_collect_data**: `applicantName` = `${workflow.input.applicantName}`
**ht_process_form**: `approved` = `${human_review_ref.output.approved}`, `reviewNotes` = `${human_review_ref.output.reviewNotes}`, `riskScore` = `${human_review_ref.output.riskScore}`

## Tests

**11 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
