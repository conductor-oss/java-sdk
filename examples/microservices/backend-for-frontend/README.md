# Backend For Frontend in Java with Conductor

Backend for Frontend pattern with platform-specific responses.

## The Problem

Different client platforms (web, mobile, TV) need different response shapes from the same backend data. A web dashboard can display a full user profile with all fields, while a mobile app needs a compact summary to conserve bandwidth. The BFF pattern solves this by fetching shared data once and then branching into platform-specific transformers.

Without orchestration, each platform ends up with its own ad-hoc endpoint that duplicates the data-fetching logic, or a single endpoint bloats with if/else branches for every client type. Adding a new platform (e.g., smart TV) means touching existing code paths and risking regressions.

## The Solution

**You just write the data-fetch and platform-transform workers. Conductor handles conditional platform routing, retry on data-fetch failures, and per-request tracing.**

Each worker represents a service boundary. Conductor manages cross-service orchestration, compensating transactions, timeout enforcement, and distributed tracing. your workers just make the service calls.

### What You Write: Workers

FetchDataWorker loads shared user data once, then TransformWebWorker and TransformMobileWorker each reshape it for their target platform. Web gets the full dataset, mobile gets a compressed payload.

| Worker | Task | What It Does |
|---|---|---|
| **FetchDataWorker** | `bff_fetch_data` | Loads the user's profile, order count, and notification count from backend services. |
| **TransformMobileWorker** | `bff_transform_mobile` | Compresses the fetched data into a compact payload optimized for small screens and low bandwidth. |
| **TransformWebWorker** | `bff_transform_web` | Returns the full dataset with all fields for rich desktop rendering. |

the workflow coordination stays the same.

### The Workflow

```
bff_fetch_data
 │
 ▼
SWITCH (switch_ref)
 ├── web: bff_transform_web
 ├── mobile: bff_transform_mobile
 └── default: bff_transform_web

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
