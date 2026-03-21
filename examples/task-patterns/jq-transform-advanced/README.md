# Jq Transform Advanced in Java with Conductor

Advanced JQ data transformations. flatten orders, aggregate by customer, classify into tiers. ## The Problem

You need to transform raw order data into customer analytics. starting with nested order objects containing customer details and line items, flattening them into a uniform structure with computed line totals, grouping by customer to calculate total spend and average order value, and finally classifying each customer into gold/silver/bronze tiers based on their spending. These are pure data transformations with no external API calls, just reshaping, aggregating, and classifying JSON.

Without a server-side transformation engine, you'd write a worker for each reshape step, deploy three separate Java classes that do nothing but manipulate JSON, and pay the operational cost of three polling workers for logic that is pure data plumbing. The transformation logic lives in Java code that is harder to iterate on than a declarative query expression.

## The Solution

**You just write JQ expressions in the workflow definition. Conductor evaluates them server-side. No workers, no polling, no deployment.**

This example uses zero workers. Every task is a JSON_JQ_TRANSFORM. a JQ expression that runs on the Conductor server. The `jq_flatten` step takes nested order objects and produces a flat list with computed line totals (`price * qty`) and order totals. The `jq_aggregate` step groups flattened orders by customer, computing order count, total spent, and average order value per customer. The `jq_classify` step assigns each customer a tier (gold for $500+, silver for $200+, bronze otherwise) and produces a tier breakdown listing customers in each category. All three transformations are declarative JQ expressions, no Java code, no worker deployment, no polling.

### What You Write: Workers

This example uses Conductor system tasks (JSON_JQ_TRANSFORM). no custom workers needed. All three transformations are declarative JQ expressions that run on the Conductor server.

### The Workflow

```
jq_flatten [JSON_JQ_TRANSFORM]
 │
 ▼
jq_aggregate [JSON_JQ_TRANSFORM]
 │
 ▼
jq_classify [JSON_JQ_TRANSFORM]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
