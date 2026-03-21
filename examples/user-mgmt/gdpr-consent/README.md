# GDPR Consent in Java Using Conductor

## The Problem

You need to manage a user's GDPR consent preferences. Presenting the available consent options (cookies, analytics, marketing), recording which options the user accepted or declined, propagating those preferences to all downstream systems that process the user's data, and creating an immutable audit trail that proves when and what the user consented to. Each step depends on the previous one's output.

If consent is recorded but downstream systems aren't updated, your analytics platform keeps tracking a user who opted out, a direct GDPR Article 7 violation. If the audit trail fails to capture the exact consent choices and timestamp, you can't demonstrate lawful basis for processing during a regulatory inquiry. Without orchestration, you'd build a monolithic consent handler that mixes consent UI rendering, database writes, cross-system propagation, and audit logging, making it impossible to add new consent categories, integrate additional downstream systems, or produce consent histories for individual users on demand.

## The Solution

**You just write the consent-presentation, recording, system-propagation, and audit workers. Conductor handles the GDPR compliance pipeline and cross-system sync.**

PresentOptionsWorker loads the consent categories available for the user: cookies, analytics tracking, marketing communications, third-party data sharing, and returns the options that need to be displayed. RecordConsentWorker saves the user's consent choices (accepted and declined categories) with a timestamp, creating the official consent record. UpdateSystemsWorker propagates the consent preferences to all 4 downstream systems, disabling analytics tracking, removing marketing email subscriptions, updating cookie policies, and adjusting data-sharing flags. AuditWorker creates an immutable audit trail entry linking the user, their exact choices, the timestamp, and a unique audit trail ID for regulatory compliance. Each worker is a standalone Java class. Conductor handles the sequencing, retries, and crash recovery.

### What You Write: Workers

PresentOptionsWorker loads consent categories, RecordConsentWorker saves choices with timestamps, UpdateSystemsWorker propagates to analytics and marketing systems, and AuditWorker creates an immutable compliance trail.

| Worker | Task | What It Does |
|---|---|---|
| **AuditWorker** | `gdc_audit` | Creates an immutable audit trail entry with a unique audit ID linking the user's consent choices and timestamp |
| **PresentOptionsWorker** | `gdc_present_options` | Loads the available consent categories (analytics, marketing, third-party, cookies) for the user |
| **RecordConsentWorker** | `gdc_record_consent` | Records the user's consent choices with a versioned timestamp, storing the full consent record |
| **UpdateSystemsWorker** | `gdc_update_systems` | Propagates consent preferences to 4 downstream systems: analytics, email, ads, and DMP |

Replace with real identity provider and database calls and ### The Workflow

```
gdc_present_options
 │
 ▼
gdc_record_consent
 │
 ▼
gdc_update_systems
 │
 ▼
gdc_audit

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
