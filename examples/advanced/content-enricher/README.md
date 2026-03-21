# Content Enricher in Java Using Conductor: Augment Messages with External Customer Data

A webhook fires: `{"customerId": "CUST-42", "orderId": "ORD-999", "amount": 1250.00}`. That's it. Your fulfillment service needs the customer's region, account tier, and credit limit to route the order. Your analytics pipeline needs the company name and lifetime value. But the event is just an ID and a dollar amount, a skeleton with no context. So your handler starts making inline API calls to the CRM, the customer database, and the geo-IP service, tangling enrichment logic with transport logic until adding one new data source means rewriting the whole consumer. This example builds a content enrichment pipeline with Conductor: extract the customer ID, look up account data from external sources, merge it into the original payload, and forward the enriched message downstream. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Incomplete Messages Need Context

An incoming order event contains a customer ID and a list of items, but the downstream analytics service needs the customer's region, account tier, and lifetime value. A signup notification has an email address, but the marketing system needs the company name, industry, and employee count. Messages arrive lean, with just an identifier, and need to be enriched with context from CRMs, geo-IP databases, or third-party APIs before they're useful downstream.

Building enrichment inline means coupling your message consumer to every data source it needs, retrying each lookup independently, and deciding what to do when one source is down but the others succeed. The enrichment logic gets tangled with the transport logic, and adding a new data source means modifying the consumer.

## The Solution

**You write the enrichment logic. Conductor handles the pipeline, retries, and state.**

`ReceiveMessageWorker` ingests the incoming message and extracts the customer ID. `LookupDataWorker` queries the configured enrichment sources for that customer. Pulling profile data, geo information, or account details. `EnrichWorker` merges the lookup results into the original message, producing an enriched payload with all the context the downstream system needs. `ForwardWorker` delivers the enriched message to the next queue or service. Conductor ensures the lookup happens after the customer ID is extracted, retries if a data source is temporarily unavailable, and records the full before-and-after so you can see exactly what was added to each message.

### What You Write: Workers

Four workers form the enrichment pipeline: message reception, external data lookup, payload merging, and downstream forwarding, each decoupled from the data sources it queries.

| Worker | Task | What It Does |
|---|---|---|
| **ReceiveMessageWorker** | `enr_receive_message` | Parses the incoming order/signup event and extracts the customer ID for downstream lookup |
| **LookupDataWorker** | `enr_lookup_data` | Queries enrichment sources (CRM, customer DB) and returns account name, tier, region, credit limit, and order history |
| **EnrichWorker** | `enr_enrich` | Merges lookup fields into the original message, producing a single enriched payload with full customer context |
| **ForwardWorker** | `enr_forward` | Delivers the enriched message to the downstream queue (e.g., `order_processing_queue`) for fulfillment or analytics |

### The Workflow

```
enr_receive_message
 │
 ▼
enr_lookup_data
 │
 ▼
enr_enrich
 │
 ▼
enr_forward

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
