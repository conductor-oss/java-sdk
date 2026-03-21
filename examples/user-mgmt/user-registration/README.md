# User Registration in Java Using Conductor : Validation, Account Creation, Confirmation, and Activation

## The Problem

You need a registration flow that guards against invalid input before creating any records. The username must meet minimum length requirements, the email must be well-formed, and neither can already be taken. Only after validation passes should the system create a user record, send a confirmation email with the new user ID, and finally flip the account to active status. Each step depends on the one before it. you can't confirm a user that was never created, and you shouldn't activate an account before confirmation is sent.

Without orchestration, you'd write a single registration endpoint that validates, inserts into the database, calls the email service, and updates the active flag. all in one transaction. If the email service times out after the record is created, the user exists but never receives confirmation. If activation fails, there's no record of where the flow stopped. Retrying means risking duplicate accounts, and debugging a stuck registration means manually querying multiple systems.

## The Solution

**You just write the validation, account-creation, confirmation, and activation workers. Conductor handles the registration sequence and user ID propagation.**

Each registration step. validation, record creation, confirmation, activation, is a simple, independent worker. Conductor runs them in strict sequence, passes the validation result into account creation, threads the generated user ID into confirmation and activation, retries any step that fails without creating duplicates, and tracks the full registration journey for every user. ### What You Write: Workers

ValidateWorker checks username length and email format, CreateWorker generates a unique user ID, ConfirmWorker sends the confirmation email, and ActivateWorker flips the account to active.

| Worker | Task | What It Does |
|---|---|---|
| **ValidateWorker** | `ur_validate` | Checks username length (minimum 3 chars), email format (contains @), and uniqueness. returns validation result and per-check breakdown |
| **CreateWorker** | `ur_create` | Creates the user record with a unique ID (USR-XXXXXXXX) and timestamps the creation |
| **ConfirmWorker** | `ur_confirm` | Sends a confirmation email to the new user's address with their user ID |
| **ActivateWorker** | `ur_activate` | Flips the account status from pending to active, making the user fully registered |

Replace with real identity provider and database calls and ### The Workflow

```
ur_validate
 │
 ▼
ur_create
 │
 ▼
ur_confirm
 │
 ▼
ur_activate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
