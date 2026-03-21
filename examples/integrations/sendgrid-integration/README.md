# Sendgrid Integration in Java Using Conductor

## Sending Personalized Campaign Emails with Tracking

Sending a marketing email involves more than calling a send API. You need to load the email template, personalize it for the recipient (inserting their name, customizing the subject line, tailoring the content), send it through SendGrid, and set up tracking so you know who opens the email. Each step depends on the previous one. you cannot personalize without a template, and you cannot track without a message ID from the send step.

Without orchestration, you would chain SendGrid API calls manually, manage template objects and message IDs between steps, and build custom tracking setup logic. Conductor sequences the pipeline and routes templates, personalized content, and message IDs between workers automatically.

## The Solution

**You just write the email workers. Template composition, recipient personalization, SendGrid delivery, and open tracking. Conductor handles template-to-tracking sequencing, SendGrid API retries, and message ID routing between send and tracking stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers handle email delivery: ComposeEmailWorker loads templates, PersonalizeWorker inserts recipient-specific content, SendEmailWorker delivers via SendGrid, and TrackOpensWorker enables engagement tracking.

| Worker | Task | What It Does |
|---|---|---|
| **ComposeEmailWorker** | `sgd_compose_email` | Composes an email from a template. |
| **PersonalizeWorker** | `sgd_personalize` | Personalizes an email for a recipient. |
| **SendEmailWorker** | `sgd_send_email` | Sends an email via SendGrid. |
| **TrackOpensWorker** | `sgd_track_opens` | Tracks email opens. |

The workers auto-detect SendGrid credentials at startup. When `SENDGRID_API_KEY` is set, SendEmailWorker uses the real SendGrid Mail Send API (v3, via `java.net.http`) to deliver emails. Without the key, it falls back to demo mode with realistic output shapes so the workflow runs end-to-end without a SendGrid account.

### The Workflow

```
sgd_compose_email
 │
 ▼
sgd_personalize
 │
 ▼
sgd_send_email
 │
 ▼
sgd_track_opens

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
