# End-to-End App in Java with Conductor: Support Ticket Pipeline. Classify, Assign, Notify

A complete Java Conductor application that processes support tickets end-to-end: classifying the ticket by category (billing, technical, account), assigning it to the right team based on classification, and notifying the customer with a confirmation email. This is a realistic mini-application that shows how all the Conductor pieces fit together: workflow definition, multiple workers, data flow between tasks, and a main entry point. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the ticket pipeline, you write the classification, assignment, and notification logic, Conductor handles sequencing, retries, durability, and observability.

## From Hello World to a Real Application

After learning the basics (workers, workflows, data flow), you need to see how all the pieces come together in a realistic application. This support ticket pipeline is small enough to understand completely but real enough to demonstrate production patterns: a workflow that processes business data, workers that make decisions based on input, and data flowing from classification through assignment to notification.

## The Solution

**You just write the ticket classification, team assignment, and customer notification logic. Conductor handles sequencing, retries, durability, and observability.**

Three workers handle the ticket lifecycle. Classification (determining category from subject and description), assignment (routing to the right team), and notification (confirming receipt to the customer). Conductor sequences them, passing the ticket ID, classification result, and assignment through the pipeline.

### What You Write: Workers

The support ticket pipeline uses three workers: classify, assign, and notify, to show how a complete Conductor application fits together from registration to execution.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyTicketWorker** | `classify_ticket` | Scans subject and description for priority keywords (e.g., "down" or "outage" = CRITICAL, "question" = LOW). Returns a priority level and the keywords that matched. |
| **AssignTicketWorker** | `assign_ticket` | Maps category to team (billing -> Finance Support, technical -> Engineering) and upgrades response time for CRITICAL (30 min) and HIGH (1 hour) tickets. |
| **NotifyCustomerWorker** | `notify_customer` | Logs a notification to the customer with ticket details (priority, assigned team, response time). Returns `sent: true` and `channel: email`. |

### The Workflow

```
classify_ticket
 │
 ▼
assign_ticket
 │
 ▼
notify_customer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
