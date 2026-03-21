# User Feedback Processing in Java Using Conductor : Collection, Classification, Routing, and Auto-Response

## The Problem

You need to handle incoming user feedback from multiple sources (in-app forms, email, support portals). Each submission must be ingested, classified as a bug report, feature request, or general feedback, assigned a priority based on severity signals like "crash" or "urgent," routed to the appropriate team (engineering for bugs, product for feature requests, support for everything else), and followed up with a personalized acknowledgment so the user knows their feedback landed.

Without orchestration, you'd chain all of this in a single service. parsing text for keywords, looking up team routing tables, composing response messages, and wrapping every step in try/catch. When classification logic changes or a new feedback channel appears, you're editing a monolith. Failures in the response step can silently swallow the whole submission, and you have no visibility into which feedback items were classified but never routed.

## The Solution

**You just write the feedback-ingestion, classification, routing, and auto-response workers. Conductor handles the processing pipeline and team routing.**

Each concern. ingestion, classification, routing, response, is a simple, independent worker. Conductor runs them in sequence, passes the classification output into routing and routing output into the response, retries any step that fails, and tracks every feedback submission end-to-end. ### What You Write: Workers

CollectFeedbackWorker ingests submissions, ClassifyFeedbackWorker categorizes by type and priority, RouteFeedbackWorker assigns to the right team, and RespondFeedbackWorker sends personalized acknowledgments.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFeedbackWorker** | `ufb_collect` | Ingests a feedback submission, assigns a unique feedback ID and timestamp |
| **ClassifyFeedbackWorker** | `ufb_classify` | Scans feedback text for keywords to determine category (bug, feature_request, general) and priority (high, medium) |
| **RouteFeedbackWorker** | `ufb_route` | Maps the classified category to an internal team (engineering, product, support) and creates a ticket |
| **RespondFeedbackWorker** | `ufb_respond` | Sends the user a personalized auto-response referencing their category and assigned team |

Replace with real identity provider and database calls and ### The Workflow

```
ufb_collect
 │
 ▼
ufb_classify
 │
 ▼
ufb_route
 │
 ▼
ufb_respond

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
