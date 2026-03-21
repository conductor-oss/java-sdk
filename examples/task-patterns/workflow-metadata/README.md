# Workflow Metadata in Java with Conductor

Demonstrates workflow metadata and search. ## The Problem

You need to tag workflow executions with searchable metadata: category, priority, team ownership, so you can query and filter them later. Conductor lets you attach custom metadata (tags, labels, description) to workflow definitions and individual executions, making it possible to search for all "billing" workflows or all "high-priority" executions across your system.

Without workflow metadata, finding specific executions means filtering by workflow name and time range alone. Metadata lets you search by business context. "show me all failed high-priority billing workflows from this week."

## The Solution

**You just write the metadata-aware processing worker. Conductor handles attaching searchable tags, categories, and priority labels to workflow executions.**

This example attaches custom metadata: category, priority, and tags, to workflow definitions and individual executions, then demonstrates searching by those fields. MetadataTaskWorker receives `category` and `priority` inputs and returns `{ processed: true }`. The example code shows how to set workflow-level metadata (owner team, description, tags) on the workflow definition, pass business context as correlation IDs or custom labels when starting executions, and then use Conductor's search API to query executions by metadata, for example, finding all failed high-priority billing workflows from the past week. The worker itself is trivial because the value is in the metadata tagging and search, not the processing logic.

### What You Write: Workers

One worker demonstrates metadata-driven categorization: MetadataTaskWorker receives category and priority inputs and returns a processing result, while the real value lies in the searchable tags, categories, and labels Conductor attaches to the workflow execution.

| Worker | Task | What It Does |
|---|---|---|
| **MetadataTaskWorker** | `md_task` | Simple metadata task worker. Receives category and priority inputs, returns processed: true. |

Workers implement their processing steps so you can see the pattern in action without external services. Replace the simulation with real processing logic. the task pattern and Conductor orchestration remain unchanged.

### The Workflow

```
md_task

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
