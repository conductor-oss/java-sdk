# DNS Management in Java with Conductor

Orchestrates safe DNS record changes using [Conductor](https://github.com/conductor-oss/conductor). This workflow plans a DNS change, validates it against existing records for conflicts, applies it to the DNS provider, and verifies propagation.

## DNS Changes Without the Risk

Updating a DNS record sounds simple until you realize a typo can take down your entire domain. The change needs to be planned, validated for conflicts (does this CNAME clash with an existing A record?), applied to the DNS provider, and then verified to have propagated globally. Doing this manually in the Route53 console at 11 PM is how outages happen.

Without orchestration, you'd wire all of this together in a single monolithic class. managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the DNS change logic. Conductor handles plan-validate-apply sequencing and propagation verification.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. your workers call the infrastructure APIs.

### What You Write: Workers

Four workers handle safe DNS changes. Planning the record update, validating against conflicts, applying to the provider, and verifying propagation.

| Worker | Task | What It Does |
|---|---|---|
| **ApplyWorker** | `dns_apply` | Pushes the validated DNS record changes to the DNS provider (e.g., Route53) |
| **PlanWorker** | `dns_plan` | Creates a DNS change plan based on the requested domain, record type, and target |
| **ValidateWorker** | `dns_validate` | Checks for conflicts with existing DNS records before applying the change |
| **VerifyWorker** | `dns_verify` | Confirms DNS propagation by querying resolvers to ensure the new records are live |

the workflow and rollback logic stay the same.

### The Workflow

```
dns_plan
 │
 ▼
dns_validate
 │
 ▼
dns_apply
 │
 ▼
dns_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
