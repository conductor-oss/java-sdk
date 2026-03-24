# Citizen Request in Java with Conductor

Processes citizen service requests (pothole repairs, streetlight outages, noise complaints): submitting, classifying by type and urgency, routing to the responsible department, resolving, and notifying the citizen.

## The Problem

You need to process a citizen service request (pothole repair, streetlight outage, noise complaint, etc.). The request is submitted, classified by type and urgency, routed to the appropriate department or crew, resolved by the responsible team, and the citizen is notified of the resolution. Misclassifying a request sends it to the wrong department; failing to notify leaves citizens wondering if their government is responsive.

Without orchestration, you'd manage service requests through a call center or web form, manually classifying and routing them to departments via email, tracking status in spreadsheets, and following up with citizens by phone. losing requests in inter-department handoffs and missing response-time SLAs.

## The Solution

**You just write the request submission, classification, department routing, resolution, and citizen notification logic. Conductor handles fulfillment retries, routing logic, and citizen request audit trails.**

Each service request concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (submit, classify, route, resolve, notify), tracking every request with timestamps and department assignments, and resuming from the last step if the process crashes.

### What You Write: Workers

Request intake, routing, fulfillment, and response workers handle citizen service requests as a traceable chain of government actions.

| Worker | Task | What It Does |
|---|---|---|
| **SubmitWorker** | `ctz_submit` | Receives the citizen's service request (e.g., pothole report) and assigns a tracking ID |
| **ClassifyWorker** | `ctz_classify` | Classifies the request type (infrastructure, sanitation, parks, etc.) for department routing |
| **RouteWorker** | `ctz_route` | Routes the classified request to the responsible department (e.g., Public Works for infrastructure) |
| **ResolveWorker** | `ctz_resolve` | The assigned department resolves the request and records the resolution (e.g., "Pothole repaired at Main St") |
| **NotifyWorker** | `ctz_notify` | Sends a notification to the citizen that their request has been resolved |

### The Workflow

```
ctz_submit
 │
 ▼
ctz_classify
 │
 ▼
ctz_route
 │
 ▼
ctz_resolve
 │
 ▼
ctz_notify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
