# Tournament Bracket in Java Using Conductor

Runs a competitive tournament from registration to finals: accepting registrations, seeding by skill, generating brackets, managing match rounds, and finalizing results with prize distribution.

## The Problem

You need to run a competitive tournament from registration to finals. The workflow accepts player registrations, seeds participants based on skill rating, generates the tournament bracket (single elimination, double elimination, or round robin), manages each round of matches, and finalizes results with prize distribution. Unseeded brackets produce lopsided first-round matchups; mismanaged rounds can deadlock the tournament.

Without orchestration, you'd build a tournament platform that manages registration, generates brackets, coordinates match scheduling, records results, and advances winners. manually handling disqualifications, byes for odd player counts, and the complex state management of a multi-round elimination bracket.

## The Solution

**You just write the registration, skill seeding, bracket generation, match round management, and prize distribution logic. Conductor handles bracket generation retries, match scheduling, and tournament progression tracking.**

Each tournament concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (register, seed, create bracket, manage rounds, finalize), tracking every tournament's complete state, and resuming from the last step if the process crashes.

### What You Write: Workers

Registration, seeding, bracket generation, and match scheduling workers each own one structural aspect of tournament organization.

| Worker | Task | What It Does |
|---|---|---|
| **CreateBracketWorker** | `tbk_create_bracket` | Generates the tournament bracket structure with round count and total matches based on the chosen format |
| **FinalizeWorker** | `tbk_finalize` | Declares the champion and runner-up, distributes prizes, and marks the tournament as completed |
| **ManageRoundsWorker** | `tbk_manage_rounds` | Manages each round of matches in the bracket, recording results and advancing winners |
| **RegisterWorker** | `tbk_register` | Registers players for the tournament and validates entry requirements |
| **SeedWorker** | `tbk_seed` | Seeds registered players by skill ranking for balanced bracket placement |

### The Workflow

```
tbk_register
 │
 ▼
tbk_seed
 │
 ▼
tbk_create_bracket
 │
 ▼
tbk_manage_rounds
 │
 ▼
tbk_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
