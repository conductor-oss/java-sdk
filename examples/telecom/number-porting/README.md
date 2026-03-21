# Number Porting in Java Using Conductor

## Why Number Porting Needs Orchestration

Porting a phone number between carriers requires precise coordination across independent systems. You submit a port request with the subscriber's phone number and destination carrier. You validate that the number is portable. confirming the number exists on the losing carrier's network and is not under contract lock. You coordinate with both the gaining and losing carriers to agree on a port window. You execute the actual port by updating routing tables in the Number Portability Administration Center (NPAC). Finally, you verify the number is reachable on the new carrier's network.

If the port execution succeeds but verification fails, the subscriber has a number that routes incorrectly and can't receive calls. If coordination fails after validation, you need to know exactly which carrier acknowledged the port window so you can cancel cleanly. Without orchestration, you'd build a fragile script that mixes NPAC API calls, carrier-to-carrier messaging, and routing database updates. making it impossible to retry a failed port execution without re-running the entire process from scratch.

## The Solution

**You just write the port request, eligibility validation, carrier coordination, number execution, and verification logic. Conductor handles carrier coordination retries, assignment sequencing, and porting audit trails.**

Each worker handles one telecom operation. Conductor manages the provisioning pipeline, activation sequencing, billing triggers, and service state tracking.

### What You Write: Workers

Port request validation, carrier coordination, number assignment, and activation workers each manage one phase of transferring a phone number between carriers.

| Worker | Task | What It Does |
|---|---|---|
| **CoordinateWorker** | `npt_coordinate` | Coordinates the port window between the gaining and losing carriers using the port ID. |
| **PortWorker** | `npt_port` | Executes the number port by updating routing tables so the phone number routes to the new carrier. |
| **RequestWorker** | `npt_request` | Submits the port request for a phone number to the destination carrier and returns a port ID. |
| **ValidateWorker** | `npt_validate` | Validates the phone number's portability with the losing carrier. checking eligibility and contract status. |
| **VerifyWorker** | `npt_verify` | Verifies the ported number is reachable on the new carrier's network after the port completes. |

### The Workflow

```
npt_request
 │
 ▼
npt_validate
 │
 ▼
npt_coordinate
 │
 ▼
npt_port
 │
 ▼
npt_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
