# CDC Pipeline in Java Using Conductor

A customer updates their shipping address at 2:03 PM. The downstream cache still shows the old address at 2:18 PM because the sync job runs on a 15-minute cron. The warehouse ships to the wrong address. The real-time price update your marketing team pushed to the products table at 11:00 AM doesn't reach the storefront until 11:15. after 200 customers have already checked out at the old price. Every minute your CDC pipeline lags is a minute your downstream systems are lying to users. This example builds a change-data-capture pipeline with Conductor that detects INSERTs, UPDATEs, and DELETEs from a source table, transforms them into structured events, publishes downstream, and confirms delivery, all orchestrated with retries and a full audit trail. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## The Problem

You need to capture every INSERT, UPDATE, and DELETE from a source database table and propagate those changes downstream as structured events. The pipeline must detect changes since a given timestamp, transform raw change records into normalized event payloads (with entity IDs, before/after values, and operation types), publish them to a downstream topic, and confirm that every message was successfully delivered. Missing a change means downstream systems go out of sync; publishing without confirmation means you cannot guarantee delivery.

Without orchestration, you'd build a single CDC polling service that queries the database change log, transforms rows inline, pushes to Kafka, and checks consumer offsets. Manually handling partial publishes when the broker is unavailable, retrying failed deliveries without re-publishing duplicates, and logging every step to debug why downstream data is stale.

## The Solution

**You just write the change-detection, transform, publish, and delivery-confirmation workers. Conductor handles pipeline sequencing, automatic retry when the broker is unavailable, and a durable record of every CDC run.**

Each CDC concern is a simple, independent worker, a plain Java class that does one thing. Conductor takes care of executing them in order (detect changes, transform, publish, confirm), retrying when the message broker is temporarily unavailable, tracking every pipeline run with full change-record details, and resuming from the last successful step if the process crashes mid-publish.

### What You Write: Workers

Four workers form the CDC pipeline: DetectChangesWorker polls a source table for inserts, updates, and deletes; TransformChangesWorker normalizes raw change records into structured events; PublishDownstreamWorker sends them to a topic; and ConfirmDeliveryWorker verifies all messages landed.

| Worker | Task | What It Does |
|---|---|---|
| **ConfirmDeliveryWorker** | `cd_confirm_delivery` | Confirms that all published CDC messages were successfully delivered. Returns a deterministic delivery report. |
| **DetectChangesWorker** | `cd_detect_changes` | Detects CDC changes from a source table since a given timestamp. Returns a set of 4 change records: INSERT, UPD |
| **PublishDownstreamWorker** | `cd_publish_downstream` | Publishes transformed CDC changes to a downstream topic. Returns fixed message IDs for deterministic behavior. |
| **TransformChangesWorker** | `cd_transform_changes` | Transforms raw CDC change records into structured event payloads with eventType, entityId, payload, previousPayload, |

Workers implement event processing with realistic payloads so you can trace the full event flow without external message brokers. Replace the simulation with real event sources, the workflow and routing logic stay the same.

### The Workflow

```
cd_detect_changes
 │
 ▼
cd_transform_changes
 │
 ▼
cd_publish_downstream
 │
 ▼
cd_confirm_delivery

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
