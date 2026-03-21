# Hello World in Java with Conductor: The Simplest Possible Workflow

The absolute minimum Conductor example. One workflow, one task, one worker. Takes a `name` as input, produces a greeting as output. This is your starting point for understanding how Conductor works: you define a workflow in JSON, write a worker in Java, and Conductor connects them. Uses [Conductor](https://github.com/conductor-oss/conductor) to run a single task.

## Your First Conductor Workflow

Before building complex pipelines, you need to understand the core loop: a workflow definition (JSON) declares tasks, a worker (Java class implementing `Worker`) polls for and executes tasks, and Conductor connects them. This example strips away everything except that core loop. One task that takes a name and returns a greeting.

After running this, you'll understand: how to define a workflow in `workflow.json`, how to implement a worker with `getTaskDefName()` and `execute()`, how input flows from the workflow to the worker via `task.getInputData()`, and how output flows back via `result.getOutputData()`.

## The Solution

**One worker, one task, one workflow. The simplest possible Conductor application.**

A single `greet` worker receives a name, produces a greeting, and returns it. Conductor handles the workflow lifecycle, task polling, and execution tracking. Even for this trivially simple case.

### What You Write: Workers

A single GreetWorker demonstrates the minimum Conductor contract: receive input via `getInputData()`, do your work, return output via `getOutputData()`.

| Worker | Task | What It Does |
|---|---|---|
| **GreetWorker** | `greet` | Takes a `name` from input, returns `"Hello, {name}! Welcome to Conductor."`. Defaults to `"World"` if name is blank or missing. |

### The Workflow

```
greet

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
