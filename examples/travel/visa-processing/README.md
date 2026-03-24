# Visa Processing in Java with Conductor

Visa processing: collect docs, validate, submit, track, receive.

## The Problem

You need to process a visa application for an employee traveling internationally. collecting required documents (passport, photos, invitation letter, financial statements), validating that all documents meet the destination country's requirements, submitting the application to the consulate or visa service, tracking the application's progress through processing phases, and receiving the approved visa and returning the passport to the employee. Each step depends on the previous one's completion.

If validation fails because a document is missing or expired, you need to notify the employee before submission to avoid consulate rejection. If the application is submitted but tracking stops polling, the employee misses their visa pickup window and the trip is cancelled. Without orchestration, you'd build a monolithic handler that mixes document management, consulate API integration, status polling, and employee notifications. making it impossible to support different visa types, test document validation rules independently, or audit which documents were submitted for which application.

## The Solution

**You just write the document collection, validation, application submission, status tracking, and visa receipt logic. Conductor handles document verification retries, submission sequencing, and visa application audit trails.**

CollectWorker gathers the required documents for the visa type and destination country. Passport scans, photos, invitation letters, and financial statements. ValidateWorker checks each document against the consulate's requirements (passport validity, photo dimensions, letter formatting). SubmitWorker sends the completed application package to the consulate or visa processing service. TrackWorker monitors the application status through processing phases (received, in review, decision pending). ReceiveWorker handles the approved visa receipt and passport return to the employee. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

Document collection, eligibility verification, application submission, and status tracking workers each manage one phase of the visa application lifecycle.

| Worker | Task | What It Does |
|---|---|---|
| **CollectWorker** | `vsp_collect` | Collect. Computes and returns application id, documents |
| **ReceiveWorker** | `vsp_receive` | Visa approved and passport returned |
| **SubmitWorker** | `vsp_submit` | Submit. Computes and returns submitted, reference number |
| **TrackWorker** | `vsp_track` | Track. Computes and returns phase, estimated date |
| **ValidateWorker** | `vsp_validate` | All documents verified |

### The Workflow

```
vsp_collect
 │
 ▼
vsp_validate
 │
 ▼
vsp_submit
 │
 ▼
vsp_track
 │
 ▼
vsp_receive

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
