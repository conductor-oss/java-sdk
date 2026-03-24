# Task Input Templates in Java with Conductor

Shows reusable parameter mapping patterns.

## The Problem

You need to wire data between tasks, a user lookup returns a profile, a context builder enriches it with permissions, and an action executor needs fields from both previous steps plus the original workflow input. Input templates let you map outputs from any previous task into the next task's input using expressions like `${task_ref.output.field}`, composing complex objects from multiple sources without writing glue code.

Without input templates, you'd write mapping logic in each worker, the context builder would need to know where the user profile came from, and the action executor would need to reach back into earlier task outputs. Input templates keep the data wiring in the workflow definition where it belongs, and workers stay decoupled.

## The Solution

**You just write the user lookup, context builder, and action execution workers. Conductor handles the cross-task data wiring via input template expressions.**

This example demonstrates Conductor's input template expressions for wiring data between tasks in a user-action pipeline. LookupUserWorker takes a userId and returns a profile (name, role, email). The workflow's input template for the next task constructs a nested `context` object by pulling `${lookup_ref.output.name}` and `${lookup_ref.output.role}` alongside the original `${workflow.input.action}`. BuildContextWorker enriches that context with computed permissions based on the role. Finally, ExecuteActionWorker's input template pulls fields from both `${lookup_ref.output}` and `${context_ref.output}` plus `${workflow.input.metadata}`. Demonstrating multi-source input composition. The workers themselves know nothing about each other; all the data wiring lives in the workflow definition's `inputParameters` blocks.

### What You Write: Workers

Three workers form a user-action pipeline wired together by input templates: LookupUserWorker fetches the user profile, BuildContextWorker enriches it with role-based permissions, and ExecuteActionWorker performs the authorized action, all data mapping between them lives in the workflow definition, not in worker code.

| Worker | Task | What It Does |
|---|---|---|
| **BuildContextWorker** | `tpl_build_context` | Enriches a request context with computed permissions based on user role. |
| **ExecuteActionWorker** | `tpl_execute_action` | Executes an action, checking permissions from the enriched context. |
| **LookupUserWorker** | `tpl_lookup_user` | Looks up a user by ID and returns their profile. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
tpl_lookup_user
 │
 ▼
tpl_build_context
 │
 ▼
tpl_execute_action

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
