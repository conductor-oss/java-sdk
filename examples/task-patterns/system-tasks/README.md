# System Tasks in Java with Conductor

Demonstrates INLINE and JSON_JQ_TRANSFORM system tasks. no workers needed.

## The Problem

You need to build an employee compensation summary: look up a user's profile (name, department, base salary, performance rating), calculate their bonus based on performance tiers (15% for ratings 4.5+, 10% for 4.0+, 5% for 3.0+), and format the results into a structured summary with compensation breakdown and performance classification. None of these steps require external API calls. it is all data lookup, calculation, and reshaping. Deploying three separate worker services for this logic is unnecessary overhead.

Without system tasks, you'd write three worker classes, each containing a few lines of logic wrapped in boilerplate (implement Worker interface, register task definition, handle polling). The user lookup is a map access, the bonus calculation is basic arithmetic with if/else, and the output formatting is JSON restructuring. Three deployed services for what is essentially three pure functions adds operational cost with no benefit.

## The Solution

**You just write INLINE JavaScript and JQ expressions in the workflow definition. Conductor runs them server-side. No workers needed for pure data lookup, calculation, and reshaping.**

This example uses zero workers. everything runs as Conductor system tasks on the server. The `lookup_user` step (INLINE/GraalJS) takes a userId and looks up the employee profile from an in-memory map, returning name, department, base salary, and performance rating. The `calculate_bonus` step (INLINE/GraalJS) applies tiered bonus rules: Gold tier (15% bonus) for ratings 4.5+, Silver (10%) for 4.0+, Bronze (5%) for 3.0+, computing the dollar amount and total compensation. The `format_output` step (JSON_JQ_TRANSFORM) reshapes the user and bonus data into a structured summary with nested compensation and performance sections using a JQ expression. No worker deployment, no polling, no Docker containers, just expressions evaluated server-side.

### What You Write: Workers

This example uses zero custom workers, all three tasks (user lookup, bonus calculation, output formatting) run as server-side INLINE and JQ system tasks with no external deployment.

This example uses Conductor system tasks. no custom workers needed. All logic runs server-side via INLINE (GraalJS) and JSON_JQ_TRANSFORM tasks.

### The Workflow

```
lookup_user [INLINE]
 │
 ▼
calculate_bonus [INLINE]
 │
 ▼
format_output [JSON_JQ_TRANSFORM]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
