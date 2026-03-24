# Inline Tasks in Java with Conductor

Demonstrates INLINE tasks. JavaScript that runs on the Conductor server with no workers.

## The Problem

You need to perform lightweight data transformations between workflow steps. computing a sum and average from a list of numbers, converting text to uppercase and generating a URL slug, classifying a score into tiers (gold/silver/bronze), and assembling a final response object. These operations are simple enough that deploying a dedicated worker service for each one is overkill. You just need a few lines of logic to run without the overhead of a separate Java process, Docker container, or network round-trip.

Without INLINE tasks, every transformation requires a deployed worker: a Java class, a task definition registration, a polling connection, and operational overhead. For a workflow with 10 small transformations, that means 10 worker classes that each contain 5 lines of logic wrapped in boilerplate. The cost of deploying, monitoring, and maintaining those workers far exceeds the complexity of the logic they contain.

## The Solution

**You just write JavaScript expressions in the workflow definition. Conductor executes them server-side via GraalJS. No workers, no polling, no deployment.**

This example uses zero workers. Every task is an INLINE task. JavaScript that executes directly on the Conductor server via GraalJS. The `math_aggregation` step computes sum, average, min, max, and count from an input number array. The `string_manipulation` step converts text to uppercase, counts words, generates a URL slug, and reverses the string. The `conditional_logic` step classifies the computed average into gold (90+), silver (80+), or bronze tiers. Finally, `build_response` assembles all results into a single structured response object. No worker deployment, no polling, no network hops. just JavaScript expressions evaluated server-side.

### What You Write: Workers

This example uses Conductor system tasks (INLINE). no custom workers needed. All four tasks are JavaScript expressions that execute directly on the Conductor server via GraalJS.

### The Workflow

```
math_aggregation [INLINE]
 │
 ▼
string_manipulation [INLINE]
 │
 ▼
conditional_logic [INLINE]
 │
 ▼
build_response [INLINE]

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
