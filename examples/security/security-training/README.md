# Implementing Security Awareness Training in Java with Conductor : Module Assignment, Phishing Simulation, Evaluation, and Compliance Reporting

## The Problem

You need to run security awareness campaigns across your organization. Each campaign involves assigning training modules to a department's employees, sending demo phishing emails to test their real-world response, evaluating who completed the training and who clicked the phishing link, and generating a compliance report showing pass/fail rates. Regulatory frameworks like SOC 2 and ISO 27001 require documented evidence that these campaigns ran and that results were recorded.

Without orchestration, you'd manage training assignments in one spreadsheet, phishing exercises in a separate tool, and manually compile results into a report. If the phishing simulation fails to send to half the department, you don't know which employees were actually tested. Compliance asks for proof that engineering completed the "secure-coding-2024" module, and you spend hours cross-referencing three different systems.

## The Solution

**You just write the LMS integration and phishing simulation logic. Conductor handles campaign sequencing so phishing tests only run after training is assigned, retries if the simulation platform is temporarily down, and timestamped evidence for SOC2 and ISO 27001 auditors.**

Each phase of the awareness campaign is a simple, independent worker. a plain Java class that does one thing. Conductor sequences them so training is assigned before phishing tests are sent, simulation results feed directly into the evaluation step, and the compliance report captures everything end-to-end. If the phishing simulation worker fails partway through, Conductor retries without re-sending to employees who already received the test. Every assignment, simulation, and evaluation result is tracked with timestamps for audit evidence.

### What You Write: Workers

Four workers run the awareness campaign: StAssignTrainingWorker assigns modules to employees, StSendPhishingSimWorker delivers demo phishing emails, StEvaluateResultsWorker analyzes completion rates and click-through data, and StReportComplianceWorker generates the compliance report for auditors.

| Worker | Task | What It Does |
|---|---|---|
| **StAssignTrainingWorker** | `st_assign_training` | Assigns security training modules to department employees. |
| **StEvaluateResultsWorker** | `st_evaluate_results` | Evaluates training completion and phishing simulation results. |
| **StReportComplianceWorker** | `st_report_compliance` | Generates training compliance report. |
| **StSendPhishingSimWorker** | `st_send_phishing_sim` | Sends phishing simulation to department employees. |

the workflow logic stays the same.

### The Workflow

```
Input -> StAssignTrainingWorker -> StEvaluateResultsWorker -> StReportComplianceWorker -> StSendPhishingSimWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
