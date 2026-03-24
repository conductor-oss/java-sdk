# CRM Agent in Java Using Conductor : Customer Lookup, History Check, Record Update, Response Generation

CRM Agent. lookup customer, check history, update record, and generate response through a sequential pipeline.

## Personalized Responses Require Full Customer Context

A customer contacts support saying "My order hasn't arrived." A generic response helps nobody. A good response requires knowing who they are (gold tier member, customer since 2019), what their history looks like (three prior shipping issues, last contacted two weeks ago about a different order), updating the CRM with this new interaction, and generating a response that acknowledges their loyalty and history.

Each step builds context for the next. the customer lookup provides the profile, the history check reveals patterns, the record update ensures continuity for future interactions, and the response generation uses all accumulated context. If the CRM API is slow, you need to retry the lookup without losing the original inquiry. And every interaction must be recorded for future reference.

## The Solution

**You write the customer lookup, history retrieval, record updates, and response generation. Conductor handles the CRM pipeline, retries on API failures, and interaction tracking.**

`LookupCustomerWorker` retrieves the customer profile by ID. name, tier, account status, and metadata. `CheckHistoryWorker` queries the interaction history for past issues, resolutions, and patterns. `UpdateRecordWorker` logs the current inquiry to the CRM with timestamp, channel, and inquiry details. `GenerateResponseWorker` produces a personalized response using the full customer context, acknowledging their tier, referencing relevant history, and addressing the specific inquiry. Conductor chains these four steps and records the complete interaction context for analytics.

### What You Write: Workers

Four workers handle the CRM interaction. Looking up the customer profile, checking interaction history, updating the record, and generating a personalized response.

| Worker | Task | What It Does |
|---|---|---|
| **CheckHistoryWorker** | `cm_check_history` | Checks the interaction history for a customer, returning recent issues, total interactions, average satisfaction, sen... |
| **GenerateResponseWorker** | `cm_generate_response` | Generates a personalized response email for the customer based on their profile, tier, inquiry, recent issues, and se... |
| **LookupCustomerWorker** | `cm_lookup_customer` | Looks up a customer by ID and returns their profile information including name, email, tier, account age, contract va... |
| **UpdateRecordWorker** | `cm_update_record` | Updates the customer's CRM record with the new interaction. Creates a ticket, increments the interaction count, and r... |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
cm_lookup_customer
 │
 ▼
cm_check_history
 │
 ▼
cm_update_record
 │
 ▼
cm_generate_response

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
