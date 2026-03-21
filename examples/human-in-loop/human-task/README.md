# Human Task with Form Input in Java Using Conductor : Data Collection, WAIT for Structured Form Submission, and Form Processing

## Workflows Need to Pause for Human Input via Forms

Some workflows require a human to fill out a form. Reviewing an application, entering data, or making a decision with structured input fields. The workflow collects initial data, pauses at a WAIT task (simulating a HUMAN task with a form schema), and the human provides structured input. The form response is then processed to determine the outcome. If processing fails, you need to retry it with the form data intact.

## The Solution

**You just write the data-collection and form-processing workers. Conductor handles the durable pause for structured human input via the form schema.**

Each worker handles one stage of the approval chain. Conductor manages task assignment, wait states, timeout escalation, and audit logging. your code handles the decision logic.

### What You Write: Workers

CollectDataWorker gathers context for the form, and ProcessFormWorker interprets the submitted fields and decision, the structured form schema and durable pause are handled by the WAIT task.

| Worker | Task | What It Does |
|---|---|---|
| **CollectDataWorker** | `ht_collect_data` | Collects initial data before the human review. gathers context needed for the form and signals readiness for human input |
| *WAIT task* | `ht_human_review` | Pauses with a form schema for the reviewer; completed via `POST /tasks/{taskId}` with structured form data (name, email, decision, comments) | Built-in Conductor WAIT. no worker needed |
| **ProcessFormWorker** | `ht_process_form` | Processes the submitted form data. reads the approved flag and form fields, determines the outcome (approved or rejected), and triggers downstream actions |

Workers implement the approval steps and human decisions so the workflow runs end-to-end without manual intervention. In production, replace the auto-approve logic with real human task assignments. the workflow structure stays the same.

### The Workflow

```
ht_collect_data
 │
 ▼
human_review_wait [WAIT]
 │
 ▼
ht_process_form

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
